# android-communicator

I. Instrukcja przygotowania środowiska do użytku lokalnego:

    1. Backend Django
        Serwer - jest to źródło danych oraz podstawa komunikacji
        Wymagane zależności: Docker

        Aby uruchomić serwer lokalny należy z poziomu folderu backend wywołać komendę:
            docker compose up --build
            (jeśli chcemy aby proces serwera był w tle należy dodać na końcu flagę "-d")
        
        Należy wiedzieć że serwer django będzie nasłuchiwał na wszystkich dostępnych lokalnie adresach - np. 127.0.0.1:8000 oraz np. 192.168.0.130 (tak jak w moim przypadku)
        Aplikacja mobilna jest domyślnie skonfigurowana na komunikację z serwerem na adresie 192.168.0.130, więc są dwa rozwiązania:
            - przekonfigurowanie własnej sieci tak aby urządzenie z którego uruchamiany jest serwer django miało adres sieci lokalnej jak wyżej
            - lub zmiana konfiguracji w kodzie aplikacji komunikatora na własny (lokalny lub zewnętrzny - bez różnicy)

    2. Serwer LiveKit
        Służy do obsługiwania połączeń wideo między klientami korzystającymi z aplikacji komunikatora
        Wymagane zależności: Brak

        Aby go uruchomić należy z poziomu root repozytorium wywołać komendę:
            ./livekit-server.exe --dev --bind 0.0.0.0

        I tak samo w przypadku adresów jak z serwerem django

    3. Aplikacja mobilna
        Aplikacja - służy do kontaktowania się ze znajomymi, prowadzenia rozmów wideo oraz chatowania na żywo z kim tylko zechcesz
        Wymagane zależności: Telefon z oprogramowaniem android

        Do modyfikowania kodu aplikacji oraz uruchomienia jej w zmodyfikowanej wersji będziesz potrzebować kompilatora (np. android studio)
        Otworzenie folderu frontend w IDE powinno poprawnie wczytać projekt aplikacji
        
        Wymagania aplikacji:
            - dostęp do internetu
            - kamera
            - mikrofon

        Aby aplikacja poprawnie komunikowała się z serwerem django należy zadbać o wspólną sieć lokalną w której telefon może bez przeszkód komunikować się z serwerem
        (Celowo nie usuwałem modułu admin w serwerze django aby można było sprawdzić np. z telefonu czy "widać" serwer - spróbuj w przeglądarce otworzyć stronę [domyślnie - http://192.168.0.130/admin])

        Jeśli widzimy serwer i spełniamy wszystkie zależności to należy jeszcze dać uprawnienia aplikacji do korzystania z funkcji telefonu (np. powiadomienia itp.)
        Aby poprawnie skonfigurować powiadomienia należy w konsoli firebase dodać aplikację oraz pobrać dwa klucze - jeden dla backendu i drugi dla aplikacji
        Aplikacja używa google-services.json który powinien znajdować się w folderze /frontend/app/src/<tutaj>
        Serwer używa <nazwa_klucza>.json który powinien znajdować się w folderze /backend/<tutaj>

        Do korzystania ze skompilowanej i domyślnie skonfigurowanej aplikacji nie potrzebujesz środowiska android studio (czy innego)
        Wystarczy że pobierzesz na telefon plik app-debug.apk oraz zainstalujesz aplikację "frontend" z domyślną zieloną ikonką androida
        Telefon prawdopodobnie zapyta lub domyślnie zabroni zainstalować tę aplikację więc aby skorzystać będzie trzeba zezwolić na instalowanie z nieznanego źródła
        
        Projekt powstał w celach edukacyjnych i nie ma na celu kradzieży danych czy uszkodzenia urządzenia
        Domyślna konfiguracja nie wykonuje zapytań wychodzących poza sieć lokalną (tylko adres serwera - 192.168.0.130)