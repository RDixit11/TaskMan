from flask import Flask, request, jsonify
from werkzeug.security import generate_password_hash, check_password_hash
import sqlite3
import os
import secrets
from datetime import datetime, timedelta
from functools import wraps

app = Flask(__name__)
app.config["SECRET_KEY"] = os.environ.get("SECRET_KEY", "twoj-sekretny-klucz-zmien-w-produkcji")

DATABASE = "uzytkownicy.db"

# ======= Stany zadań =======
STANY_ZADAN = {"niezrobione", "w_trakcie", "zrobione"}


# ======= DB =======
def get_db_connection():
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row
    return conn


def table_exists(conn, table_name: str) -> bool:
    cur = conn.cursor()
    cur.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
        (table_name,),
    )
    return cur.fetchone() is not None


def init_db():
    conn = get_db_connection()
    cur = conn.cursor()

    # --- uzytkownicy ---
    if not table_exists(conn, "uzytkownicy"):
        cur.execute(
            """
            CREATE TABLE uzytkownicy (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                login TEXT UNIQUE NOT NULL,
                haslo TEXT NOT NULL,
                utworzono TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """
        )

    # --- sesje ---
    if not table_exists(conn, "sesje"):
        cur.execute(
            """
            CREATE TABLE sesje (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uzytkownik_id INTEGER NOT NULL,
                token_csrf TEXT UNIQUE NOT NULL,
                utworzono TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                wygasa TIMESTAMP NOT NULL,
                FOREIGN KEY (uzytkownik_id) REFERENCES uzytkownicy(id)
            )
            """
        )

    # --- single_user_tasks_tables ---
    if not table_exists(conn, "single_user_tasks_tables"):
        cur.execute(
            """
            CREATE TABLE single_user_tasks_tables (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uzytkownik_id INTEGER NOT NULL,
                nazwa TEXT NOT NULL,
                utworzono TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                zaktualizowano TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (uzytkownik_id) REFERENCES uzytkownicy(id)
            )
            """
        )

    # --- single_user_tasks ---
    if not table_exists(conn, "single_user_tasks"):
        cur.execute(
            """
            CREATE TABLE single_user_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                lista_id INTEGER NOT NULL,
                tytul TEXT NOT NULL,
                opis TEXT,
                stan TEXT DEFAULT 'niezrobione',
                utworzono TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                zaktualizowano TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (lista_id) REFERENCES single_user_tasks_tables(id)
            )
            """
        )

    # ======= MULTI USER =======
    if not table_exists(conn, "multi_user_task_list"):
        cur.execute(
            """
            CREATE TABLE multi_user_task_list (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nazwa TEXT NOT NULL,
                utworzono TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                zaktualizowano TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """
        )

    # brak roli; każdy członek ma te same uprawnienia
    if not table_exists(conn, "multi_user_task_list_members"):
        cur.execute(
            """
            CREATE TABLE multi_user_task_list_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                lista_id INTEGER NOT NULL,
                uzytkownik_id INTEGER NOT NULL,
                utworzono TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (lista_id) REFERENCES multi_user_task_list(id),
                FOREIGN KEY (uzytkownik_id) REFERENCES uzytkownicy(id),
                UNIQUE (lista_id, uzytkownik_id)
            )
            """
        )

    if not table_exists(conn, "tasks_for_multi_user"):
        cur.execute(
            """
            CREATE TABLE tasks_for_multi_user (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                lista_id INTEGER NOT NULL,
                tytul TEXT NOT NULL,
                opis TEXT,
                stan TEXT DEFAULT 'niezrobione',
                przypisany_uzytkownik_id INTEGER NULL,
                utworzono TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                zaktualizowano TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (lista_id) REFERENCES multi_user_task_list(id),
                FOREIGN KEY (przypisany_uzytkownik_id) REFERENCES uzytkownicy(id)
            )
            """
        )

    conn.commit()
    conn.close()


# ======= Walidacje =======
def validate_login(login: str):
    if not login or len(login) < 3 or len(login) > 50:
        return False, "Login musi mieć od 3 do 50 znaków."
    if not all(c.isalnum() or c == "_" for c in login):
        return False, "Login może zawierać tylko litery, cyfry i podkreślniki."
    return True, ""


def validate_password(password: str):
    if not password or len(password) < 8:
        return False, "Hasło musi mieć co najmniej 8 znaków."
    return True, ""


def validate_task_table_name(nazwa: str):
    if not nazwa or len(nazwa) < 1 or len(nazwa) > 255:
        return False, "Nazwa musi mieć od 1 do 255 znaków."
    return True, ""


def validate_task_title(tytul: str):
    if not tytul or len(tytul) < 1 or len(tytul) > 255:
        return False, "Tytuł musi mieć od 1 do 255 znaków."
    return True, ""


def validate_task_description(opis: str | None):
    if opis is not None and len(opis) > 5000:
        return False, "Opis nie może przekraczać 5000 znaków."
    return True, ""


def validate_task_state(stan: str):
    if stan not in STANY_ZADAN:
        return False, "Stan musi być: niezrobione, w_trakcie, zrobione."
    return True, ""


# ======= Auth =======
def generate_csrf_token():
    return secrets.token_urlsafe(32)


def uwierzytelniaj_token(f):
    @wraps(f)
    def wrapper(*args, **kwargs):
        token_csrf = None

        dane = request.get_json(silent=True)
        if dane:
            token_csrf = dane.get("token_csrf")

        if not token_csrf:
            token_csrf = request.headers.get("X-CSRF-Token")

        if not token_csrf:
            return (
                jsonify(
                    {
                        "sukces": False,
                        "blad": "Brak tokenu CSRF. Podaj go w polu 'token_csrf' lub nagłówku 'X-CSRF-Token'.",
                    }
                ),
                401,
            )

        conn = get_db_connection()
        cur = conn.cursor()
        try:
            cur.execute(
                """
                SELECT uzytkownik_id
                FROM sesje
                WHERE token_csrf = ? AND wygasa > datetime('now')
                """,
                (token_csrf,),
            )
            sesja = cur.fetchone()
            if not sesja:
                return jsonify({"sukces": False, "blad": "Token jest nieprawidłowy lub wygasł."}), 401

            kwargs["uzytkownik_id"] = sesja["uzytkownik_id"]
            return f(*args, **kwargs)
        finally:
            conn.close()

    return wrapper


# ======= Multi: uprawnienia =======
def czy_jest_czlonkiem_listy(conn, lista_id: int, uzytkownik_id: int) -> bool:
    cur = conn.cursor()
    cur.execute(
        """
        SELECT 1 FROM multi_user_task_list_members
        WHERE lista_id = ? AND uzytkownik_id = ?
        """,
        (lista_id, uzytkownik_id),
    )
    return cur.fetchone() is not None


def wymagaj_czlonkostwa(conn, lista_id: int, uzytkownik_id: int):
    if not czy_jest_czlonkiem_listy(conn, lista_id, uzytkownik_id):
        return False, (jsonify({"sukces": False, "blad": "Brak dostępu do tej listy."}), 403)
    return True, None


def liczba_czlonkow(conn, lista_id: int) -> int:
    cur = conn.cursor()
    cur.execute(
        "SELECT COUNT(*) AS c FROM multi_user_task_list_members WHERE lista_id = ?",
        (lista_id,),
    )
    return int(cur.fetchone()["c"])


# ===================== AUTH ENDPOINTS =====================
@app.route("/api/rejestracja", methods=["POST"])
def rejestracja():
    dane = request.get_json() or {}
    login = (dane.get("login") or "").strip()
    haslo = (dane.get("haslo") or "").strip()

    ok, msg = validate_login(login)
    if not ok:
        return jsonify({"sukces": False, "blad": msg}), 400

    ok, msg = validate_password(haslo)
    if not ok:
        return jsonify({"sukces": False, "blad": msg}), 400

    haslo_hash = generate_password_hash(haslo)

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        try:
            cur.execute("INSERT INTO uzytkownicy (login, haslo) VALUES (?, ?)", (login, haslo_hash))
            conn.commit()
        except sqlite3.IntegrityError:
            conn.rollback()
            return jsonify({"sukces": False, "blad": "Login już istnieje w bazie danych."}), 409

        return (
            jsonify(
                {
                    "sukces": True,
                    "wiadomosc": "Użytkownik zarejestrowany pomyślnie.",
                    "uzytkownik_id": cur.lastrowid,
                    "login": login,
                }
            ),
            201,
        )
    finally:
        conn.close()


@app.route("/api/logowanie", methods=["POST"])
def logowanie():
    dane = request.get_json() or {}
    login = (dane.get("login") or "").strip()
    haslo = (dane.get("haslo") or "").strip()

    if not login or not haslo:
        return jsonify({"sukces": False, "blad": "Login i hasło są wymagane."}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute("SELECT id, haslo FROM uzytkownicy WHERE login = ?", (login,))
        user = cur.fetchone()
        if not user or not check_password_hash(user["haslo"], haslo):
            return jsonify({"sukces": False, "blad": "Nieprawidłowy login lub hasło."}), 401

        token = generate_csrf_token()
        wygasa = datetime.utcnow() + timedelta(hours=24)

        cur.execute(
            "INSERT INTO sesje (uzytkownik_id, token_csrf, wygasa) VALUES (?, ?, ?)",
            (user["id"], token, wygasa),
        )
        conn.commit()

        return (
            jsonify(
                {
                    "sukces": True,
                    "wiadomosc": "Zalogowano pomyślnie.",
                    "uzytkownik_id": user["id"],
                    "token_csrf": token,
                    "wygasa_za_godzin": 24,
                }
            ),
            200,
        )
    finally:
        conn.close()


@app.route("/api/sprawdz-token", methods=["POST"])
def sprawdz_token():
    dane = request.get_json() or {}
    token = (dane.get("token_csrf") or "").strip()
    if not token:
        return jsonify({"sukces": False, "blad": "Token CSRF jest wymagany."}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            SELECT uzytkownik_id FROM sesje
            WHERE token_csrf = ? AND wygasa > datetime('now')
            """,
            (token,),
        )
        row = cur.fetchone()
        if not row:
            return jsonify({"sukces": False, "blad": "Token jest nieprawidłowy lub wygasł."}), 401
        return jsonify({"sukces": True, "wiadomosc": "Token jest prawidłowy.", "uzytkownik_id": row["uzytkownik_id"]}), 200
    finally:
        conn.close()


# ===================== SINGLE LISTS =====================
@app.route("/api/listy-zadan", methods=["GET"])
@uwierzytelniaj_token
def pobierz_listy_zadan(uzytkownik_id):
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            SELECT id, nazwa, utworzono, zaktualizowano
            FROM single_user_tasks_tables
            WHERE uzytkownik_id = ?
            ORDER BY zaktualizowano DESC
            """,
            (uzytkownik_id,),
        )
        rows = cur.fetchall()
        return jsonify({"sukces": True, "listy": [dict(r) for r in rows], "ilosc": len(rows)}), 200
    finally:
        conn.close()


@app.route("/api/listy-zadan", methods=["POST"])
@uwierzytelniaj_token
def stworz_liste_zadan(uzytkownik_id):
    dane = request.get_json() or {}
    nazwa = (dane.get("nazwa") or "").strip()

    ok, msg = validate_task_table_name(nazwa)
    if not ok:
        return jsonify({"sukces": False, "blad": msg}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            "INSERT INTO single_user_tasks_tables (uzytkownik_id, nazwa) VALUES (?, ?)",
            (uzytkownik_id, nazwa),
        )
        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Lista utworzona.", "id": cur.lastrowid, "nazwa": nazwa}), 201
    finally:
        conn.close()


@app.route("/api/listy-zadan/<int:lista_id>", methods=["GET"])
@uwierzytelniaj_token
def pobierz_liste_zadan(lista_id, uzytkownik_id):
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            SELECT id, nazwa, utworzono, zaktualizowano
            FROM single_user_tasks_tables
            WHERE id = ? AND uzytkownik_id = ?
            """,
            (lista_id, uzytkownik_id),
        )
        row = cur.fetchone()
        if not row:
            return jsonify({"sukces": False, "blad": "Lista nie istnieje lub brak dostępu."}), 404
        return jsonify({"sukces": True, **dict(row)}), 200
    finally:
        conn.close()


@app.route("/api/listy-zadan/<int:lista_id>", methods=["PUT"])
@uwierzytelniaj_token
def edytuj_liste_zadan(lista_id, uzytkownik_id):
    dane = request.get_json() or {}
    nazwa = (dane.get("nazwa") or "").strip()

    ok, msg = validate_task_table_name(nazwa)
    if not ok:
        return jsonify({"sukces": False, "blad": msg}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            "SELECT 1 FROM single_user_tasks_tables WHERE id = ? AND uzytkownik_id = ?",
            (lista_id, uzytkownik_id),
        )
        if not cur.fetchone():
            return jsonify({"sukces": False, "blad": "Lista nie istnieje lub brak dostępu."}), 404

        cur.execute(
            """
            UPDATE single_user_tasks_tables
            SET nazwa = ?, zaktualizowano = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            (nazwa, lista_id),
        )
        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Zmieniono nazwę listy.", "id": lista_id, "nazwa": nazwa}), 200
    finally:
        conn.close()


@app.route("/api/listy-zadan/<int:lista_id>", methods=["DELETE"])
@uwierzytelniaj_token
def usun_liste_zadan(lista_id, uzytkownik_id):
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            "SELECT 1 FROM single_user_tasks_tables WHERE id = ? AND uzytkownik_id = ?",
            (lista_id, uzytkownik_id),
        )
        if not cur.fetchone():
            return jsonify({"sukces": False, "blad": "Lista nie istnieje lub brak dostępu."}), 404

        cur.execute("DELETE FROM single_user_tasks_tables WHERE id = ?", (lista_id,))
        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Usunięto listę.", "id": lista_id}), 200
    finally:
        conn.close()


# ===================== SINGLE TASKS =====================
@app.route("/api/listy-zadan/<int:lista_id>/zadania", methods=["GET"])
@uwierzytelniaj_token
def pobierz_zadania(lista_id, uzytkownik_id):
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            "SELECT 1 FROM single_user_tasks_tables WHERE id = ? AND uzytkownik_id = ?",
            (lista_id, uzytkownik_id),
        )
        if not cur.fetchone():
            return jsonify({"sukces": False, "blad": "Lista nie istnieje lub brak dostępu."}), 404

        cur.execute(
            """
            SELECT id, lista_id, tytul, opis, stan, utworzono, zaktualizowano
            FROM single_user_tasks
            WHERE lista_id = ?
            ORDER BY zaktualizowano DESC
            """,
            (lista_id,),
        )
        rows = cur.fetchall()
        return jsonify({"sukces": True, "zadania": [dict(r) for r in rows], "ilosc": len(rows)}), 200
    finally:
        conn.close()


@app.route("/api/listy-zadan/<int:lista_id>/zadania", methods=["POST"])
@uwierzytelniaj_token
def stworz_zadanie(lista_id, uzytkownik_id):
    dane = request.get_json() or {}
    tytul = (dane.get("tytul") or "").strip()
    opis = dane.get("opis", None)
    if isinstance(opis, str):
        opis = opis.strip()
        if opis == "":
            opis = None

    ok, msg = validate_task_title(tytul)
    if not ok:
        return jsonify({"sukces": False, "blad": msg}), 400
    ok, msg = validate_task_description(opis)
    if not ok:
        return jsonify({"sukces": False, "blad": msg}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            "SELECT 1 FROM single_user_tasks_tables WHERE id = ? AND uzytkownik_id = ?",
            (lista_id, uzytkownik_id),
        )
        if not cur.fetchone():
            return jsonify({"sukces": False, "blad": "Lista nie istnieje lub brak dostępu."}), 404

        cur.execute(
            """
            INSERT INTO single_user_tasks (lista_id, tytul, opis, stan)
            VALUES (?, ?, ?, 'niezrobione')
            """,
            (lista_id, tytul, opis),
        )
        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Utworzono zadanie.", "id": cur.lastrowid, "stan": "niezrobione"}), 201
    finally:
        conn.close()


@app.route("/api/zadania/<int:zadanie_id>", methods=["GET"])
@uwierzytelniaj_token
def pobierz_zadanie(zadanie_id, uzytkownik_id):
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            SELECT z.id, z.lista_id, z.tytul, z.opis, z.stan, z.utworzono, z.zaktualizowano
            FROM single_user_tasks z
            JOIN single_user_tasks_tables l ON l.id = z.lista_id
            WHERE z.id = ? AND l.uzytkownik_id = ?
            """,
            (zadanie_id, uzytkownik_id),
        )
        row = cur.fetchone()
        if not row:
            return jsonify({"sukces": False, "blad": "Zadanie nie istnieje lub brak dostępu."}), 404
        return jsonify({"sukces": True, **dict(row)}), 200
    finally:
        conn.close()


@app.route("/api/zadania/<int:zadanie_id>", methods=["PUT"])
@uwierzytelniaj_token
def edytuj_zadanie(zadanie_id, uzytkownik_id):
    dane = request.get_json() or {}
    nowy_tytul = dane.get("tytul", None)
    nowy_opis = dane.get("opis", None)

    if nowy_tytul is not None:
        nowy_tytul = (nowy_tytul or "").strip()
        ok, msg = validate_task_title(nowy_tytul)
        if not ok:
            return jsonify({"sukces": False, "blad": msg}), 400

    if isinstance(nowy_opis, str):
        nowy_opis = nowy_opis.strip()
        if nowy_opis == "":
            nowy_opis = None
    if nowy_opis is not None:
        ok, msg = validate_task_description(nowy_opis)
        if not ok:
            return jsonify({"sukces": False, "blad": msg}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            SELECT z.id
            FROM single_user_tasks z
            JOIN single_user_tasks_tables l ON l.id = z.lista_id
            WHERE z.id = ? AND l.uzytkownik_id = ?
            """,
            (zadanie_id, uzytkownik_id),
        )
        if not cur.fetchone():
            return jsonify({"sukces": False, "blad": "Zadanie nie istnieje lub brak dostępu."}), 404

        fields = []
        params = []
        if nowy_tytul is not None:
            fields.append("tytul = ?")
            params.append(nowy_tytul)
        if nowy_opis is not None:
            fields.append("opis = ?")
            params.append(nowy_opis)

        if not fields:
            return jsonify({"sukces": False, "blad": "Brak zmian do zapisania."}), 400

        fields.append("zaktualizowano = CURRENT_TIMESTAMP")
        params.append(zadanie_id)

        cur.execute(f"UPDATE single_user_tasks SET {', '.join(fields)} WHERE id = ?", params)
        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Zaktualizowano zadanie."}), 200
    finally:
        conn.close()


@app.route("/api/zadania/<int:zadanie_id>", methods=["DELETE"])
@uwierzytelniaj_token
def usun_zadanie(zadanie_id, uzytkownik_id):
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            SELECT z.id
            FROM single_user_tasks z
            JOIN single_user_tasks_tables l ON l.id = z.lista_id
            WHERE z.id = ? AND l.uzytkownik_id = ?
            """,
            (zadanie_id, uzytkownik_id),
        )
        if not cur.fetchone():
            return jsonify({"sukces": False, "blad": "Zadanie nie istnieje lub brak dostępu."}), 404

        cur.execute("DELETE FROM single_user_tasks WHERE id = ?", (zadanie_id,))
        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Usunięto zadanie.", "id": zadanie_id}), 200
    finally:
        conn.close()


@app.route("/api/zadania/<int:zadanie_id>/stan", methods=["PATCH"])
@uwierzytelniaj_token
def zmien_stan_zadania(zadanie_id, uzytkownik_id):
    dane = request.get_json() or {}
    stan = (dane.get("stan") or "").strip()

    ok, msg = validate_task_state(stan)
    if not ok:
        return jsonify({"sukces": False, "blad": msg}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            SELECT z.id
            FROM single_user_tasks z
            JOIN single_user_tasks_tables l ON l.id = z.lista_id
            WHERE z.id = ? AND l.uzytkownik_id = ?
            """,
            (zadanie_id, uzytkownik_id),
        )
        if not cur.fetchone():
            return jsonify({"sukces": False, "blad": "Zadanie nie istnieje lub brak dostępu."}), 404

        cur.execute(
            """
            UPDATE single_user_tasks
            SET stan = ?, zaktualizowano = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            (stan, zadanie_id),
        )
        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Zmieniono stan zadania.", "id": zadanie_id, "stan": stan}), 200
    finally:
        conn.close()


# ===================== MULTI LISTS =====================
@app.route("/api/multi-listy", methods=["POST"])
@uwierzytelniaj_token
def multi_utworz_liste(uzytkownik_id):
    dane = request.get_json() or {}
    nazwa = (dane.get("nazwa") or "").strip()
    if not nazwa:
        return jsonify({"sukces": False, "blad": "Nazwa jest wymagana."}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute("INSERT INTO multi_user_task_list (nazwa) VALUES (?)", (nazwa,))
        lista_id = cur.lastrowid

        # twórca jako pierwszy członek
        cur.execute(
            """
            INSERT INTO multi_user_task_list_members (lista_id, uzytkownik_id)
            VALUES (?, ?)
            """,
            (lista_id, uzytkownik_id),
        )

        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Utworzono multi-listę.", "id": lista_id, "nazwa": nazwa}), 201
    finally:
        conn.close()


@app.route("/api/multi-listy", methods=["GET"])
@uwierzytelniaj_token
def multi_pobierz_listy(uzytkownik_id):
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            SELECT l.id, l.nazwa, l.utworzono, l.zaktualizowano
            FROM multi_user_task_list l
            JOIN multi_user_task_list_members m ON m.lista_id = l.id
            WHERE m.uzytkownik_id = ?
            ORDER BY l.zaktualizowano DESC
            """,
            (uzytkownik_id,),
        )
        rows = cur.fetchall()
        return jsonify({"sukces": True, "listy": [dict(r) for r in rows], "ilosc": len(rows)}), 200
    finally:
        conn.close()


@app.route("/api/multi-listy/<int:lista_id>/members", methods=["GET"])
@uwierzytelniaj_token
def multi_pobierz_czlonkow(lista_id, uzytkownik_id):
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        ok, resp = wymagaj_czlonkostwa(conn, lista_id, uzytkownik_id)
        if not ok:
            return resp

        cur.execute(
            """
            SELECT m.uzytkownik_id, u.login, m.utworzono
            FROM multi_user_task_list_members m
            JOIN uzytkownicy u ON u.id = m.uzytkownik_id
            WHERE m.lista_id = ?
            ORDER BY u.login ASC
            """,
            (lista_id,),
        )
        rows = cur.fetchall()
        return jsonify({"sukces": True, "members": [dict(r) for r in rows], "ilosc": len(rows)}), 200
    finally:
        conn.close()


@app.route("/api/multi-listy/<int:lista_id>/members", methods=["POST"])
@uwierzytelniaj_token
def multi_dodaj_czlonka(lista_id, uzytkownik_id):
    dane = request.get_json() or {}
    login = (dane.get("login") or "").strip()
    if not login:
        return jsonify({"sukces": False, "blad": "Login jest wymagany."}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        ok, resp = wymagaj_czlonkostwa(conn, lista_id, uzytkownik_id)
        if not ok:
            return resp

        cur.execute("SELECT id FROM uzytkownicy WHERE login = ?", (login,))
        user = cur.fetchone()
        if not user:
            return jsonify({"sukces": False, "blad": "Nie znaleziono użytkownika o takim loginie."}), 404

        try:
            cur.execute(
                """
                INSERT INTO multi_user_task_list_members (lista_id, uzytkownik_id)
                VALUES (?, ?)
                """,
                (lista_id, user["id"]),
            )
        except sqlite3.IntegrityError:
            return jsonify({"sukces": False, "blad": "Ten użytkownik już jest w tej liście."}), 409

        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Dodano użytkownika do listy."}), 201
    finally:
        conn.close()


@app.route("/api/multi-listy/<int:lista_id>/members/<int:member_user_id>", methods=["DELETE"])
@uwierzytelniaj_token
def multi_usun_czlonka(lista_id, member_user_id, uzytkownik_id):
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        ok, resp = wymagaj_czlonkostwa(conn, lista_id, uzytkownik_id)
        if not ok:
            return resp

        cur.execute(
            "SELECT 1 FROM multi_user_task_list_members WHERE lista_id = ? AND uzytkownik_id = ?",
            (lista_id, member_user_id),
        )
        target = cur.fetchone()
        if not target:
            return jsonify({"sukces": False, "blad": "Ten użytkownik nie jest członkiem listy."}), 404

        # Nie pozwól usunąć ostatniego członka listy
        if liczba_czlonkow(conn, lista_id) <= 1:
            return jsonify({"sukces": False, "blad": "Nie można usunąć ostatniego członka listy."}), 400

        # Jeśli usuwany user miał przypisane zadania w tej liście: odpinamy
        cur.execute(
            """
            UPDATE tasks_for_multi_user
            SET przypisany_uzytkownik_id = NULL, zaktualizowano = CURRENT_TIMESTAMP
            WHERE lista_id = ? AND przypisany_uzytkownik_id = ?
            """,
            (lista_id, member_user_id),
        )

        cur.execute(
            "DELETE FROM multi_user_task_list_members WHERE lista_id = ? AND uzytkownik_id = ?",
            (lista_id, member_user_id),
        )
        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Usunięto użytkownika z listy."}), 200
    finally:
        conn.close()


# ===================== MULTI TASKS =====================
@app.route("/api/multi-listy/<int:lista_id>/zadania", methods=["GET"])
@uwierzytelniaj_token
def multi_pobierz_zadania(lista_id, uzytkownik_id):
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        ok, resp = wymagaj_czlonkostwa(conn, lista_id, uzytkownik_id)
        if not ok:
            return resp

        cur.execute(
            """
            SELECT z.id, z.lista_id, z.tytul, z.opis, z.stan,
                   z.przypisany_uzytkownik_id,
                   u.login AS przypisany_login,
                   z.utworzono, z.zaktualizowano
            FROM tasks_for_multi_user z
            LEFT JOIN uzytkownicy u ON u.id = z.przypisany_uzytkownik_id
            WHERE z.lista_id = ?
            ORDER BY z.zaktualizowano DESC
            """,
            (lista_id,),
        )
        rows = cur.fetchall()
        return jsonify({"sukces": True, "zadania": [dict(r) for r in rows], "ilosc": len(rows)}), 200
    finally:
        conn.close()


@app.route("/api/multi-listy/<int:lista_id>/zadania", methods=["POST"])
@uwierzytelniaj_token
def multi_dodaj_zadanie(lista_id, uzytkownik_id):
    dane = request.get_json() or {}
    tytul = (dane.get("tytul") or "").strip()
    opis = dane.get("opis", None)
    if isinstance(opis, str):
        opis = opis.strip()
        if opis == "":
            opis = None

    ok, msg = validate_task_title(tytul)
    if not ok:
        return jsonify({"sukces": False, "blad": msg}), 400
    ok, msg = validate_task_description(opis)
    if not ok:
        return jsonify({"sukces": False, "blad": msg}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        ok, resp = wymagaj_czlonkostwa(conn, lista_id, uzytkownik_id)
        if not ok:
            return resp

        cur.execute(
            """
            INSERT INTO tasks_for_multi_user (lista_id, tytul, opis, stan, przypisany_uzytkownik_id)
            VALUES (?, ?, ?, 'niezrobione', NULL)
            """,
            (lista_id, tytul, opis),
        )
        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Dodano zadanie.", "id": cur.lastrowid, "stan": "niezrobione"}), 201
    finally:
        conn.close()


@app.route("/api/multi-zadania/<int:zadanie_id>", methods=["GET"])
@uwierzytelniaj_token
def multi_pobierz_zadanie(zadanie_id, uzytkownik_id):
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute("SELECT lista_id FROM tasks_for_multi_user WHERE id = ?", (zadanie_id,))
        row = cur.fetchone()
        if not row:
            return jsonify({"sukces": False, "blad": "Nie znaleziono zadania."}), 404

        lista_id = row["lista_id"]
        ok, resp = wymagaj_czlonkostwa(conn, lista_id, uzytkownik_id)
        if not ok:
            return resp

        cur.execute(
            """
            SELECT z.id, z.lista_id, z.tytul, z.opis, z.stan,
                   z.przypisany_uzytkownik_id,
                   u.login AS przypisany_login,
                   z.utworzono, z.zaktualizowano
            FROM tasks_for_multi_user z
            LEFT JOIN uzytkownicy u ON u.id = z.przypisany_uzytkownik_id
            WHERE z.id = ?
            """,
            (zadanie_id,),
        )
        task = cur.fetchone()
        return jsonify({"sukces": True, **dict(task)}), 200
    finally:
        conn.close()


@app.route("/api/multi-zadania/<int:zadanie_id>", methods=["PUT"])
@uwierzytelniaj_token
def multi_edytuj_zadanie(zadanie_id, uzytkownik_id):
    dane = request.get_json() or {}
    tytul = dane.get("tytul", None)
    opis = dane.get("opis", None)

    if tytul is not None:
        tytul = (tytul or "").strip()
        ok, msg = validate_task_title(tytul)
        if not ok:
            return jsonify({"sukces": False, "blad": msg}), 400

    if isinstance(opis, str):
        opis = opis.strip()
        if opis == "":
            opis = None
    if opis is not None:
        ok, msg = validate_task_description(opis)
        if not ok:
            return jsonify({"sukces": False, "blad": msg}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute("SELECT lista_id FROM tasks_for_multi_user WHERE id = ?", (zadanie_id,))
        row = cur.fetchone()
        if not row:
            return jsonify({"sukces": False, "blad": "Nie znaleziono zadania."}), 404

        lista_id = row["lista_id"]
        ok, resp = wymagaj_czlonkostwa(conn, lista_id, uzytkownik_id)
        if not ok:
            return resp

        fields = []
        params = []
        if tytul is not None:
            fields.append("tytul = ?")
            params.append(tytul)
        if opis is not None:
            fields.append("opis = ?")
            params.append(opis)

        if not fields:
            return jsonify({"sukces": False, "blad": "Brak zmian do zapisania."}), 400

        fields.append("zaktualizowano = CURRENT_TIMESTAMP")
        params.append(zadanie_id)

        cur.execute(f"UPDATE tasks_for_multi_user SET {', '.join(fields)} WHERE id = ?", params)
        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Zaktualizowano zadanie."}), 200
    finally:
        conn.close()


@app.route("/api/multi-zadania/<int:zadanie_id>", methods=["DELETE"])
@uwierzytelniaj_token
def multi_usun_zadanie(zadanie_id, uzytkownik_id):
    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute("SELECT lista_id FROM tasks_for_multi_user WHERE id = ?", (zadanie_id,))
        row = cur.fetchone()
        if not row:
            return jsonify({"sukces": False, "blad": "Nie znaleziono zadania."}), 404

        lista_id = row["lista_id"]
        ok, resp = wymagaj_czlonkostwa(conn, lista_id, uzytkownik_id)
        if not ok:
            return resp

        cur.execute("DELETE FROM tasks_for_multi_user WHERE id = ?", (zadanie_id,))
        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Usunięto zadanie."}), 200
    finally:
        conn.close()


@app.route("/api/multi-zadania/<int:zadanie_id>/stan", methods=["PATCH"])
@uwierzytelniaj_token
def multi_zmien_stan(zadanie_id, uzytkownik_id):
    dane = request.get_json() or {}
    stan = (dane.get("stan") or "").strip()
    ok, msg = validate_task_state(stan)
    if not ok:
        return jsonify({"sukces": False, "blad": msg}), 400

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute("SELECT lista_id FROM tasks_for_multi_user WHERE id = ?", (zadanie_id,))
        row = cur.fetchone()
        if not row:
            return jsonify({"sukces": False, "blad": "Nie znaleziono zadania."}), 404

        lista_id = row["lista_id"]
        ok, resp = wymagaj_czlonkostwa(conn, lista_id, uzytkownik_id)
        if not ok:
            return resp

        cur.execute(
            """
            UPDATE tasks_for_multi_user
            SET stan = ?, zaktualizowano = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            (stan, zadanie_id),
        )
        conn.commit()
        return jsonify({"sukces": True, "wiadomosc": "Zmieniono stan zadania.", "id": zadanie_id, "stan": stan}), 200
    finally:
        conn.close()


@app.route("/api/multi-zadania/<int:zadanie_id>/assign", methods=["PATCH"])
@uwierzytelniaj_token
def multi_przypisz_zadanie(zadanie_id, uzytkownik_id):
    dane = request.get_json() or {}
    przypisany_id = dane.get("przypisany_uzytkownik_id", None)  # może być None

    conn = get_db_connection()
    cur = conn.cursor()
    try:
        cur.execute("SELECT lista_id FROM tasks_for_multi_user WHERE id = ?", (zadanie_id,))
        task = cur.fetchone()
        if not task:
            return jsonify({"sukces": False, "blad": "Nie znaleziono zadania."}), 404

        lista_id = task["lista_id"]
        ok, resp = wymagaj_czlonkostwa(conn, lista_id, uzytkownik_id)
        if not ok:
            return resp

        # jeśli przypisujemy do konkretnego usera, musi być członkiem tej listy
        if przypisany_id is not None:
            cur.execute(
                """
                SELECT 1 FROM multi_user_task_list_members
                WHERE lista_id = ? AND uzytkownik_id = ?
                """,
                (lista_id, przypisany_id),
            )
            if not cur.fetchone():
                return jsonify({"sukces": False, "blad": "Nie można przypisać zadania do użytkownika spoza tej listy."}), 400

        cur.execute(
            """
            UPDATE tasks_for_multi_user
            SET przypisany_uzytkownik_id = ?, zaktualizowano = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            (przypisany_id, zadanie_id),
        )
        conn.commit()
        return jsonify(
            {
                "sukces": True,
                "wiadomosc": "Zmieniono przypisanie zadania.",
                "zadanie_id": zadanie_id,
                "przypisany_uzytkownik_id": przypisany_id,
            }
        ), 200
    finally:
        conn.close()


if __name__ == "__main__":
    init_db()
    app.run(debug=True, port=5000)