# Backend API – Task Manager (Flask + SQLite, bez ORM)

Panie, ten dokument opisuje **wszystkie endpointy** dostępne w backendzie: rejestracja/logowanie, single‑user listy i zadania oraz multi‑user listy i zadania (bez ról – każdy członek ma takie same uprawnienia).

Bazowy URL:
- `http://localhost:5000`

Prefix API:
- `/api`

---

## 1) Wymagania i uruchomienie

### Backend
Wymagane pakiety:
```bash
pip install flask werkzeug
```

Uruchomienie:
```bash
python app.py
```

Baza danych:
- SQLite: plik `uzytkownicy.db`
- Tworzona automatycznie przy pierwszym uruchomieniu

### (Opcjonalnie) CLI do testowania
Wymagane:
```bash
pip install requests
```

Uruchomienie:
```bash
python cli.py
```

---

## 2) Format odpowiedzi

API zwraca JSON.

Typowe pola:
- `sukces` – `true/false`
- `wiadomosc` – informacja przy sukcesie
- `blad` – informacja przy błędzie

---

## 3) Uwierzytelnianie (token CSRF)

Po poprawnym logowaniu backend zwraca `token_csrf`.  
Wszystkie endpointy poza rejestracją i logowaniem wymagają tokenu.

### Jak przekazać token
Są 2 sposoby:

**A) Nagłówek (zalecane dla GET):**
- `X-CSRF-Token: <token>`

**B) Body JSON (wygodne dla POST/PUT/PATCH/DELETE):**
```json
{
  "token_csrf": "<token>"
}
```

Jeżeli tokenu brakuje:
- `401` oraz komunikat: `Brak tokenu CSRF...`

Token ma czas ważności (w kodzie: 24 godziny).

---

## 4) Kody HTTP (orientacyjnie)

- `200` OK
- `201` Created
- `400` błędne dane wejściowe
- `401` brak/niepoprawny token lub błędne dane logowania
- `403` brak dostępu (np. ktoś nie jest członkiem multi‑listy)
- `404` nie znaleziono zasobu
- `409` konflikt (np. login już istnieje / użytkownik już jest w multi‑liście)
- `500` błąd serwera

---

# 5) Endpointy: Rejestracja i logowanie

## 5.1 Rejestracja użytkownika
**POST** `/api/rejestracja`

Body:
```json
{
  "login": "jan_kowalski",
  "haslo": "BezpieczneHaslo123"
}
```

Odpowiedzi:
- `201` – utworzono użytkownika
- `409` – login już istnieje
- `400` – walidacja danych

Przykład:
```bash
curl -X POST http://localhost:5000/api/rejestracja \
  -H "Content-Type: application/json" \
  -d '{"login":"jan_kowalski","haslo":"BezpieczneHaslo123"}'
```

---

## 5.2 Logowanie użytkownika (zwraca token)
**POST** `/api/logowanie`

Body:
```json
{
  "login": "jan_kowalski",
  "haslo": "BezpieczneHaslo123"
}
```

Odpowiedź (przykład):
```json
{
  "sukces": true,
  "wiadomosc": "Zalogowano pomyślnie.",
  "uzytkownik_id": 1,
  "token_csrf": "....",
  "wygasa_za_godzin": 24
}
```

Przykład:
```bash
curl -X POST http://localhost:5000/api/logowanie \
  -H "Content-Type: application/json" \
  -d '{"login":"jan_kowalski","haslo":"BezpieczneHaslo123"}'
```

---

## 5.3 Sprawdzenie tokenu
**POST** `/api/sprawdz-token`

Body:
```json
{
  "token_csrf": "twoj-token"
}
```

Przykład:
```bash
curl -X POST http://localhost:5000/api/sprawdz-token \
  -H "Content-Type: application/json" \
  -d '{"token_csrf":"twoj-token"}'
```

---

# 6) Single-user: listy zadań (`single_user_tasks_tables`)

Każda lista należy do jednego użytkownika (właściciela).

## 6.1 Pobierz wszystkie listy zalogowanego użytkownika
**GET** `/api/listy-zadan`  
Token: TAK

Przykład:
```bash
curl -X GET http://localhost:5000/api/listy-zadan \
  -H "X-CSRF-Token: twoj-token"
```

---

## 6.2 Utwórz listę
**POST** `/api/listy-zadan`  
Token: TAK

Body:
```json
{
  "token_csrf": "twoj-token",
  "nazwa": "Zakupy"
}
```

---

## 6.3 Pobierz szczegóły listy
**GET** `/api/listy-zadan/<lista_id>`  
Token: TAK

---

## 6.4 Zmień nazwę listy
**PUT** `/api/listy-zadan/<lista_id>`  
Token: TAK

Body:
```json
{
  "token_csrf": "twoj-token",
  "nazwa": "Nowa nazwa"
}
```

---

## 6.5 Usuń listę
**DELETE** `/api/listy-zadan/<lista_id>`  
Token: TAK

Uwaga:
- W tej implementacji usuwanie listy nie usuwa automatycznie zadań (zależy od dalszych decyzji / migracji FK).

---

# 7) Single-user: zadania (`single_user_tasks`)

Stan zadania jest jednym z:
- `niezrobione` (domyślnie)
- `w_trakcie`
- `zrobione`

## 7.1 Pobierz zadania z listy
**GET** `/api/listy-zadan/<lista_id>/zadania`  
Token: TAK

---

## 7.2 Dodaj zadanie do listy
**POST** `/api/listy-zadan/<lista_id>/zadania`  
Token: TAK

Body:
```json
{
  "token_csrf": "twoj-token",
  "tytul": "Kupić chleb",
  "opis": "Dłuższy opis (opcjonalny)"
}
```

---

## 7.3 Pobierz jedno zadanie
**GET** `/api/zadania/<zadanie_id>`  
Token: TAK

---

## 7.4 Edytuj zadanie (tytuł i/lub opis)
**PUT** `/api/zadania/<zadanie_id>`  
Token: TAK

Body (można wysłać tylko jedno pole):
```json
{
  "token_csrf": "twoj-token",
  "tytul": "Nowy tytuł",
  "opis": "Nowy opis"
}
```

---

## 7.5 Usuń zadanie
**DELETE** `/api/zadania/<zadanie_id>`  
Token: TAK

---

## 7.6 Zmień stan zadania
**PATCH** `/api/zadania/<zadanie_id>/stan`  
Token: TAK

Body:
```json
{
  "token_csrf": "twoj-token",
  "stan": "w_trakcie"
}
```

---

# 8) Multi-user: współdzielone listy zadań

W multi‑listach:
- **nie ma ról**
- **każdy członek** ma takie same uprawnienia:
  - może dodawać i usuwać członków,
  - może dodawać/edytować/usuwać zadania,
  - może przypisywać/odpinać zadania.

Tabele:
- `multi_user_task_list`
- `multi_user_task_list_members`
- `tasks_for_multi_user`

Ważna zasada:
- Jeśli użytkownik nie jest członkiem multi‑listy → `403`.

---

## 8.1 Utwórz multi-listę
**POST** `/api/multi-listy`  
Token: TAK

Body:
```json
{
  "token_csrf": "twoj-token",
  "nazwa": "Projekt A"
}
```

Efekt:
- twórca zostaje pierwszym członkiem listy.

---

## 8.2 Pobierz multi-listy, do których należysz
**GET** `/api/multi-listy`  
Token: TAK

---

## 8.3 Pobierz członków multi-listy
**GET** `/api/multi-listy/<lista_id>/members`  
Token: TAK

---

## 8.4 Dodaj członka do multi-listy (po loginie)
**POST** `/api/multi-listy/<lista_id>/members`  
Token: TAK

Body:
```json
{
  "token_csrf": "twoj-token",
  "login": "inny_login"
}
```

Odpowiedzi:
- `201` dodano
- `409` już jest członkiem
- `404` nie znaleziono użytkownika

---

## 8.5 Usuń członka z multi-listy (po user_id)
**DELETE** `/api/multi-listy/<lista_id>/members/<member_user_id>`  
Token: TAK

Uwaga:
- backend blokuje usunięcie **ostatniego członka listy** (`400`).

Dodatkowe zachowanie:
- jeśli usuwany użytkownik miał przypisane zadania w tej multi‑liście, backend je **odpina** (`przypisany_uzytkownik_id = NULL`).

---

# 9) Multi-user: zadania (`tasks_for_multi_user`)

Stan zadania:
- `niezrobione` (domyślnie)
- `w_trakcie`
- `zrobione`

Dodatkowe pole:
- `przypisany_uzytkownik_id` – może być `null` (nieprzypisane)

---

## 9.1 Pobierz zadania w multi-liście
**GET** `/api/multi-listy/<lista_id>/zadania`  
Token: TAK

Zwraca też:
- `przypisany_login` (jeśli przypisane)

---

## 9.2 Dodaj zadanie do multi-listy
**POST** `/api/multi-listy/<lista_id>/zadania`  
Token: TAK

Body:
```json
{
  "token_csrf": "twoj-token",
  "tytul": "Napisać testy",
  "opis": "Opcjonalny opis"
}
```

---

## 9.3 Pobierz jedno multi-zadanie
**GET** `/api/multi-zadania/<zadanie_id>`  
Token: TAK

---

## 9.4 Edytuj multi-zadanie
**PUT** `/api/multi-zadania/<zadanie_id>`  
Token: TAK

Body:
```json
{
  "token_csrf": "twoj-token",
  "tytul": "Nowy tytuł",
  "opis": "Nowy opis"
}
```

---

## 9.5 Usuń multi-zadanie
**DELETE** `/api/multi-zadania/<zadanie_id>`  
Token: TAK

---

## 9.6 Zmień stan multi-zadania
**PATCH** `/api/multi-zadania/<zadanie_id>/stan`  
Token: TAK

Body:
```json
{
  "token_csrf": "twoj-token",
  "stan": "zrobione"
}
```

---

## 9.7 Przypisz / odpisz zadanie (wybór użytkownika)

**PATCH** `/api/multi-zadania/<zadanie_id>/assign`  
Token: TAK

### Przypisanie do użytkownika
Body:
```json
{
  "token_csrf": "twoj-token",
  "przypisany_uzytkownik_id": 5
}
```

Warunek:
- `przypisany_uzytkownik_id` musi należeć do członka tej multi‑listy, w przeciwnym razie `400`.

### Odpięcie (ustawienie NULL)
Body:
```json
{
  "token_csrf": "twoj-token",
  "przypisany_uzytkownik_id": null
}
```

---

# 10) Minimalny scenariusz użycia

1. Zarejestruj:
   - `POST /api/rejestracja`
2. Zaloguj:
   - `POST /api/logowanie` → zapisz `token_csrf`
3. Single:
   - `POST /api/listy-zadan` → utwórz listę
   - `POST /api/listy-zadan/<id>/zadania` → dodaj zadanie
4. Multi:
   - `POST /api/multi-listy` → utwórz listę współdzieloną
   - `POST /api/multi-listy/<id>/members` → dodaj członka po loginie
   - `POST /api/multi-listy/<id>/zadania` → dodaj zadanie
   - `PATCH /api/multi-zadania/<task_id>/assign` → przypisz zadanie do członka

---

Jeśli Pan chce, dopiszę jeszcze sekcję “Diagnostyka” (najczęstsze błędy: token, 403 w multi‑liście, 409 przy członkach).