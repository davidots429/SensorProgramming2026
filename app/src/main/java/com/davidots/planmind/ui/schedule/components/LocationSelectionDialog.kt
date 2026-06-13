package com.davidots.planmind.ui.schedule.components

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL


// 구글 공식 Geocoding API를 이용한 역지오코딩 비동기 함수 (좌표를 입력받아 실제 한국어 주소명으로 변환)
suspend fun reverseGeocodeWithGoogle(latLng: LatLng, apiKey: String): String = withContext(Dispatchers.IO) {
    try {
        val urlString = "https://maps.googleapis.com/maps/api/geocode/json?latlng=${latLng.latitude},${latLng.longitude}&key=$apiKey&language=ko"
        val response = URL(urlString).readText()
        val jsonObject = JSONObject(response)
        if (jsonObject.getString("status") == "OK") {
            val results = jsonObject.getJSONArray("results")
            if (results.length() > 0) return@withContext results.getJSONObject(0).getString("formatted_address")
        }
        ""
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}


// 구글 지도 기반의 장소 검색 및 좌표 지정 다이얼로그 컴포저블
@Composable
fun LocationSelectionDialog(
    initialLatLng: LatLng?,
    onDismiss: () -> Unit,
    onLocationSelected: (latLng: LatLng, placeName: String, address: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // API Key 초기화 및 로드
    val apiKey = remember(context) {
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val key = appInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
        if (!Places.isInitialized()) Places.initialize(context, key)
        key
    }
    val placesClient = remember(context) { Places.createClient(context) }

    var selectedLatLng by remember { mutableStateOf(initialLatLng) }
    var placeNameToReturn by remember { mutableStateOf("") }
    var placeAddressToReturn by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    val defaultLocation = LatLng(37.5665, 126.9780) // 서울 시청 기본 좌표
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng ?: defaultLocation, 15f)
    }

    // 내 위치로 이동하는 헬퍼 함수
    val fetchCurrentLocation = {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
                    }
                }
            }
        } catch (e: SecurityException) {}
    }

    // 검색어 입력 시 Places API 자동완성 요청 (디바운스 적용)
    LaunchedEffect(searchQuery) {
        if (searchQuery.length < 2) {
            predictions = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        val request = FindAutocompletePredictionsRequest.builder().setQuery(searchQuery).setCountries("KR").build()
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response -> predictions = response.autocompletePredictions }
            .addOnFailureListener { predictions = emptyList() }
    }

    LaunchedEffect(Unit) {
        if (initialLatLng == null) fetchCurrentLocation()
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("위치 지정 취소") },
            text = { Text("위치 지정을 취소하시겠습니까? 현재 화면이 닫힙니다.") },
            confirmButton = {
                TextButton(onClick = { showCancelConfirm = false; onDismiss() }) { Text("예", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showCancelConfirm = false }) { Text("아니오") } }
        )
    }

    Dialog(onDismissRequest = { showCancelConfirm = true }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // [지도 렌더링 영역]
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                onMapClick = { latLng ->
                    selectedLatLng = latLng
                    keyboardController?.hide()
                    predictions = emptyList()
                    coroutineScope.launch {
                        val googleAddress = reverseGeocodeWithGoogle(latLng, apiKey)
                        placeAddressToReturn = googleAddress
                        placeNameToReturn = googleAddress
                    }
                }
            ) {
                selectedLatLng?.let { latLng -> Marker(state = MarkerState(position = latLng), title = "선택한 위치") }
            }

            // [상단 검색창 및 자동완성 리스트 영역]
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter).statusBarsPadding()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("장소 검색 (예: 강남역, 코엑스)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                AnimatedVisibility(visible = predictions.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                            items(predictions) { prediction ->
                                Column(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val placeRequest = FetchPlaceRequest.builder(prediction.placeId, listOf(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS)).build()
                                        placesClient.fetchPlace(placeRequest).addOnSuccessListener { response ->
                                            response.place.latLng?.let { latLng ->
                                                selectedLatLng = latLng
                                                placeNameToReturn = response.place.name ?: ""
                                                placeAddressToReturn = response.place.address ?: ""
                                                searchQuery = placeNameToReturn
                                                predictions = emptyList()
                                                keyboardController?.hide()
                                                coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f)) }
                                            }
                                        }
                                    }.padding(16.dp)
                                ) {
                                    Text(text = prediction.getPrimaryText(null).toString(), fontWeight = FontWeight.Bold)
                                    Text(text = prediction.getSecondaryText(null).toString(), fontSize = 12.sp, color = Color.Gray)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // [내 위치로 이동 버튼]
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp, end = 16.dp), contentAlignment = Alignment.BottomEnd) {
                FloatingActionButton(onClick = { fetchCurrentLocation() }, containerColor = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = "현재 위치로 이동")
                }
            }

            // [하단 컨트롤 버튼]
            Surface(modifier = Modifier.align(Alignment.BottomCenter), shadowElevation = 8.dp) {
                Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
                    Button(
                        onClick = {
                            selectedLatLng?.let {
                                val finalName = placeNameToReturn.ifBlank { searchQuery }
                                onLocationSelected(it, finalName, placeAddressToReturn)
                            }
                        },
                        enabled = selectedLatLng != null
                    ) { Text("이 위치 선택") }
                }
            }
        }
    }
}