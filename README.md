이 프로젝트는 Android Studio를 사용하여 개발된 간단한 카메라 애플리케이션 데모입니다. 

이 앱은 사용자가 'Scan' 버튼을 클릭하면 카메라를 활성화하고, 사진을 캡처한 뒤, 해당 이미지를 서버에 자동으로 업로드합니다.



- 기능
    - 카메라 접근: 사용자가 카메라를 사용할 수 있도록 권한을 요청합니다.
    - 사진 캡처: 'Scan' 버튼을 통해 카메라 인터페이스로 이동하고 사진을 캡처할 수 있습니다.
    - 이미지 업로드: 캡처된 이미지를 Flask 서버로 자동 업로드합니다.
  
- 사용 기술
    Android Studio: 앱 개발 및 디자인을 위한 통합 개발 환경(IDE).
    Kotlin: 앱 로직 및 UI 인터렉션을 구현하는데 사용된 프로그래밍 언어.
    CameraX: 안드로이드 Jetpack의 일부로, 카메라 앱 개발을 보다 쉽게 해주는 라이브러리.
    Retrofit: HTTP API를 자바 인터페이스로 변환하는 타입-세이프한 HTTP 클라이언트.
    OkHttp: HTTP 및 HTTP/2 클라이언트.
    Gson: 자바 객체를 JSON 표현식으로 변환하는 라이브러리.
  
- 설치 방법
    1. 이 리포지토리를 클론합니다:
        git clone https://github.com/yourusername/android-camera-app-demo.git
    2. Android Studio에서 클론된 프로젝트를 엽니다.
    3. Build > Rebuild Project를 선택하여 프로젝트를 빌드합니다.
    4. 에뮬레이터를 실행하거나 안드로이드 기기에 앱을 설치하여 테스트합니다.
    ("http://ec2 public IP:8080/" //flask 서버 주소 입력)

- 구성 파일 설명
    - MainActivity.kt: 앱의 메인 화면과 초기 권한 체크 로직을 관리합니다.
    - CameraActivity.kt: 카메라 인터페이스와 사진 캡처 기능을 구현합니다.
    - AndroidManifest.xml: 애플리케이션에 필요한 권한과 액티비티 설정을 정의합니다.
    - build.gradle: 프로젝트와 앱 모듈의 빌드 설정을 정의합니다.
