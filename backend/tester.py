import requests
import json
from typing import Optional, Dict, Any

BASE_URL = "http://localhost:5000/api"
TOKEN_FILE = ".auth_token"


class TaskManagerCLI:
    def __init__(self):
        self.token_csrf: Optional[str] = None
        self.uzytkownik_id: Optional[int] = None
        self.load_token()

    # ---------- token ----------
    def load_token(self):
        try:
            with open(TOKEN_FILE, "r", encoding="utf-8") as f:
                data = json.load(f)
                self.token_csrf = data.get("token_csrf")
                self.uzytkownik_id = data.get("uzytkownik_id")
        except FileNotFoundError:
            pass
        except Exception:
            # jeżeli plik uszkodzony
            self.token_csrf = None
            self.uzytkownik_id = None

    def save_token(self, token_csrf: str, uzytkownik_id: int):
        with open(TOKEN_FILE, "w", encoding="utf-8") as f:
            json.dump({"token_csrf": token_csrf, "uzytkownik_id": uzytkownik_id}, f)
        self.token_csrf = token_csrf
        self.uzytkownik_id = uzytkownik_id

    def clear_token(self):
        import os

        try:
            os.remove(TOKEN_FILE)
        except FileNotFoundError:
            pass
        self.token_csrf = None
        self.uzytkownik_id = None

    # ---------- http ----------
    def make_request(
        self,
        method: str,
        endpoint: str,
        data: Optional[Dict] = None,
        use_token: bool = True,
    ) -> Optional[Dict[str, Any]]:
        url = f"{BASE_URL}{endpoint}"
        headers = {"Content-Type": "application/json"}
        payload = data if data is not None else {}

        if use_token and self.token_csrf:
            if method.upper() == "GET":
                headers["X-CSRF-Token"] = self.token_csrf
            else:
                payload["token_csrf"] = self.token_csrf

        try:
            m = method.upper()
            if m == "GET":
                resp = requests.get(url, headers=headers, timeout=10)
            elif m == "POST":
                resp = requests.post(url, headers=headers, json=payload, timeout=10)
            elif m == "PUT":
                resp = requests.put(url, headers=headers, json=payload, timeout=10)
            elif m == "PATCH":
                resp = requests.patch(url, headers=headers, json=payload, timeout=10)
            elif m == "DELETE":
                resp = requests.delete(url, headers=headers, json=payload, timeout=10)
            else:
                print("❌ Nieznana metoda HTTP.")
                return None

            if resp.status_code in (200, 201):
                return resp.json()

            # błąd:
            try:
                j = resp.json()
                print(f"❌ Błąd ({resp.status_code}): {j.get('blad', 'Nieznany błąd')}")
            except Exception:
                print(f"❌ Błąd ({resp.status_code}): {resp.text[:200]}")
            return None

        except requests.exceptions.ConnectionError:
            print("❌ Brak połączenia z backendem. Uruchom app.py.")
            return None
        except requests.exceptions.Timeout:
            print("❌ Timeout. Backend nie odpowiedział.")
            return None
        except Exception as e:
            print(f"❌ Błąd żądania: {e}")
            return None

    # ---------- ui ----------
    def clear_screen(self):
        import os

        os.system("clear" if os.name == "posix" else "cls")

    def header(self, text: str):
        print("\n" + "=" * 72)
        print(text.center(72))
        print("=" * 72)

    def sep(self):
        print("-" * 72)

    def pause(self):
        input("\nENTER aby kontynuować...")

    def read_int(self, prompt: str) -> Optional[int]:
        try:
            return int(input(prompt).strip())
        except ValueError:
            return None

    # ---------- auth ----------
    def rejestracja(self):
        self.header("REJESTRACJA")
        login = input("\nLogin: ").strip()
        haslo = input("Hasło: ").strip()
        haslo2 = input("Powtórz hasło: ").strip()
        if haslo != haslo2:
            print("❌ Hasła się nie zgadzają.")
            self.pause()
            return

        res = self.make_request("POST", "/rejestracja", {"login": login, "haslo": haslo}, use_token=False)
        if res and res.get("sukces"):
            print("✅ Rejestracja OK.")
            print(f"   user_id: {res.get('uzytkownik_id')}")
        self.pause()

    def logowanie(self) -> bool:
        self.header("LOGOWANIE")
        login = input("\nLogin: ").strip()
        haslo = input("Hasło: ").strip()

        res = self.make_request("POST", "/logowanie", {"login": login, "haslo": haslo}, use_token=False)
        if res and res.get("sukces"):
            self.save_token(res["token_csrf"], res["uzytkownik_id"])
            print("✅ Zalogowano.")
            print(f"   user_id: {self.uzytkownik_id}")
            print(f"   token: {self.token_csrf[:24]}...")
            self.pause()
            return True

        self.pause()
        return False

    # ---------- SINGLE: listy ----------
    def s_listy(self):
        self.header("SINGLE: LISTY")
        res = self.make_request("GET", "/listy-zadan")
        if res and res.get("sukces"):
            listy = res.get("listy", [])
            if not listy:
                print("\nBrak list.")
            else:
                for l in listy:
                    print(f"\nID: {l['id']} | {l['nazwa']}")
                    print(f"utworzono: {l['utworzono']} | zaktualizowano: {l['zaktualizowano']}")
        self.pause()

    def s_listy_dodaj(self):
        self.header("SINGLE: DODAJ LISTĘ")
        nazwa = input("\nNazwa: ").strip()
        res = self.make_request("POST", "/listy-zadan", {"nazwa": nazwa})
        if res and res.get("sukces"):
            print("✅ Utworzono listę.")
            print(f"ID: {res.get('id')}")
        self.pause()

    def s_listy_usun(self):
        self.header("SINGLE: USUŃ LISTĘ")
        lista_id = self.read_int("\nID listy: ")
        if lista_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        if input("Potwierdź (tak/nie): ").strip().lower() != "tak":
            print("❌ Anulowano.")
            self.pause()
            return
        res = self.make_request("DELETE", f"/listy-zadan/{lista_id}", {})
        if res and res.get("sukces"):
            print("✅ Usunięto listę.")
        self.pause()

    # ---------- SINGLE: zadania ----------
    def s_zadania_w_liscie(self):
        self.header("SINGLE: ZADANIA W LIŚCIE")
        lista_id = self.read_int("\nID listy: ")
        if lista_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        res = self.make_request("GET", f"/listy-zadan/{lista_id}/zadania")
        if res and res.get("sukces"):
            zad = res.get("zadania", [])
            if not zad:
                print("\nBrak zadań.")
            else:
                for z in zad:
                    print(f"\nID: {z['id']} | [{z['stan']}] {z['tytul']}")
                    if z.get("opis"):
                        print(f"opis: {z['opis']}")
        self.pause()

    def s_zadanie_dodaj(self):
        self.header("SINGLE: DODAJ ZADANIE")
        lista_id = self.read_int("\nID listy: ")
        if lista_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        tytul = input("Tytuł: ").strip()
        opis = input("Opis (opcjonalnie): ").strip()
        res = self.make_request("POST", f"/listy-zadan/{lista_id}/zadania", {"tytul": tytul, "opis": opis or None})
        if res and res.get("sukces"):
            print("✅ Dodano zadanie.")
            print(f"ID: {res.get('id')}")
        self.pause()

    def s_zadanie_usun(self):
        self.header("SINGLE: USUŃ ZADANIE")
        zad_id = self.read_int("\nID zadania: ")
        if zad_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        if input("Potwierdź (tak/nie): ").strip().lower() != "tak":
            print("❌ Anulowano.")
            self.pause()
            return
        res = self.make_request("DELETE", f"/zadania/{zad_id}", {})
        if res and res.get("sukces"):
            print("✅ Usunięto zadanie.")
        self.pause()

    def s_zadanie_stan(self):
        self.header("SINGLE: ZMIEŃ STAN")
        zad_id = self.read_int("\nID zadania: ")
        if zad_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        print("\n1) niezrobione\n2) w_trakcie\n3) zrobione")
        w = input("Wybór: ").strip()
        mapa = {"1": "niezrobione", "2": "w_trakcie", "3": "zrobione"}
        if w not in mapa:
            print("❌ Zły wybór.")
            self.pause()
            return
        res = self.make_request("PATCH", f"/zadania/{zad_id}/stan", {"stan": mapa[w]})
        if res and res.get("sukces"):
            print("✅ Zmieniono stan.")
        self.pause()

    # ---------- MULTI: listy ----------
    def m_listy(self):
        self.header("MULTI: MOJE LISTY")
        res = self.make_request("GET", "/multi-listy")
        if res and res.get("sukces"):
            listy = res.get("listy", [])
            if not listy:
                print("\nBrak multi-list.")
            else:
                for l in listy:
                    print(f"\nID: {l['id']} | {l['nazwa']}")
                    print(f"utworzono: {l['utworzono']} | zaktualizowano: {l['zaktualizowano']}")
        self.pause()

    def m_listy_dodaj(self):
        self.header("MULTI: UTWÓRZ LISTĘ")
        nazwa = input("\nNazwa: ").strip()
        res = self.make_request("POST", "/multi-listy", {"nazwa": nazwa})
        if res and res.get("sukces"):
            print("✅ Utworzono multi-listę.")
            print(f"ID: {res.get('id')}")
        self.pause()

    def m_members(self):
        self.header("MULTI: CZŁONKOWIE LISTY")
        lista_id = self.read_int("\nID listy: ")
        if lista_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        res = self.make_request("GET", f"/multi-listy/{lista_id}/members")
        if res and res.get("sukces"):
            members = res.get("members", [])
            if not members:
                print("\nBrak członków? (nie powinno się zdarzyć)")
            else:
                for m in members:
                    print(f"\nuser_id={m['uzytkownik_id']} | login={m['login']} | od={m['utworzono']}")
        self.pause()

    def m_member_add(self):
        self.header("MULTI: DODAJ CZŁONKA (każdy członek może)")
        lista_id = self.read_int("\nID listy: ")
        if lista_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        login = input("Login użytkownika do dodania: ").strip()
        res = self.make_request("POST", f"/multi-listy/{lista_id}/members", {"login": login})
        if res and res.get("sukces"):
            print("✅ Dodano członka.")
        self.pause()

    def m_member_remove(self):
        self.header("MULTI: USUŃ CZŁONKA (każdy członek może)")
        lista_id = self.read_int("\nID listy: ")
        if lista_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        member_id = self.read_int("ID użytkownika do usunięcia (user_id): ")
        if member_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        if input("Potwierdź (tak/nie): ").strip().lower() != "tak":
            print("❌ Anulowano.")
            self.pause()
            return
        res = self.make_request("DELETE", f"/multi-listy/{lista_id}/members/{member_id}", {})
        if res and res.get("sukces"):
            print("✅ Usunięto członka.")
        self.pause()

    # ---------- MULTI: zadania ----------
    def m_zadania_w_liscie(self):
        self.header("MULTI: ZADANIA W LIŚCIE")
        lista_id = self.read_int("\nID listy: ")
        if lista_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        res = self.make_request("GET", f"/multi-listy/{lista_id}/zadania")
        if res and res.get("sukces"):
            zad = res.get("zadania", [])
            if not zad:
                print("\nBrak zadań.")
            else:
                for z in zad:
                    przyp = z.get("przypisany_login") or "-"
                    print(f"\nID: {z['id']} | [{z['stan']}] {z['tytul']} | przypisany: {przyp}")
                    if z.get("opis"):
                        print(f"opis: {z['opis']}")
        self.pause()

    def m_zadanie_dodaj(self):
        self.header("MULTI: DODAJ ZADANIE")
        lista_id = self.read_int("\nID listy: ")
        if lista_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        tytul = input("Tytuł: ").strip()
        opis = input("Opis (opcjonalnie): ").strip()
        res = self.make_request("POST", f"/multi-listy/{lista_id}/zadania", {"tytul": tytul, "opis": opis or None})
        if res and res.get("sukces"):
            print("✅ Dodano zadanie.")
            print(f"ID: {res.get('id')}")
        self.pause()

    def m_zadanie_edytuj(self):
        self.header("MULTI: EDYTUJ ZADANIE")
        zad_id = self.read_int("\nID zadania: ")
        if zad_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        t = input("Nowy tytuł (ENTER=bez zmian): ").strip()
        o = input("Nowy opis (ENTER=bez zmian): ").strip()
        data = {}
        if t:
            data["tytul"] = t
        if o:
            data["opis"] = o
        if not data:
            print("❌ Brak zmian.")
            self.pause()
            return
        res = self.make_request("PUT", f"/multi-zadania/{zad_id}", data)
        if res and res.get("sukces"):
            print("✅ Zaktualizowano.")
        self.pause()

    def m_zadanie_usun(self):
        self.header("MULTI: USUŃ ZADANIE")
        zad_id = self.read_int("\nID zadania: ")
        if zad_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        if input("Potwierdź (tak/nie): ").strip().lower() != "tak":
            print("❌ Anulowano.")
            self.pause()
            return
        res = self.make_request("DELETE", f"/multi-zadania/{zad_id}", {})
        if res and res.get("sukces"):
            print("✅ Usunięto.")
        self.pause()

    def m_zadanie_stan(self):
        self.header("MULTI: ZMIEŃ STAN")
        zad_id = self.read_int("\nID zadania: ")
        if zad_id is None:
            print("❌ Złe ID.")
            self.pause()
            return
        print("\n1) niezrobione\n2) w_trakcie\n3) zrobione")
        w = input("Wybór: ").strip()
        mapa = {"1": "niezrobione", "2": "w_trakcie", "3": "zrobione"}
        if w not in mapa:
            print("❌ Zły wybór.")
            self.pause()
            return
        res = self.make_request("PATCH", f"/multi-zadania/{zad_id}/stan", {"stan": mapa[w]})
        if res and res.get("sukces"):
            print("✅ Zmieniono stan.")
        self.pause()

    def m_zadanie_przypisz(self):
        self.header("MULTI: PRZYPISZ / ODPISZ")
        zad_id = self.read_int("\nID zadania: ")
        if zad_id is None:
            print("❌ Złe ID.")
            self.pause()
            return

        print("\n1) Przypisz do mnie")
        print("2) Przypisz do innego user_id (musi być członkiem listy)")
        print("3) Odpisz (NULL)")
        w = input("Wybór: ").strip()

        if w == "1":
            przyp = self.uzytkownik_id
        elif w == "2":
            przyp = self.read_int("Podaj user_id: ")
            if przyp is None:
                print("❌ Złe user_id.")
                self.pause()
                return
        elif w == "3":
            przyp = None
        else:
            print("❌ Zły wybór.")
            self.pause()
            return

        res = self.make_request("PATCH", f"/multi-zadania/{zad_id}/assign", {"przypisany_uzytkownik_id": przyp})
        if res and res.get("sukces"):
            print("✅ Zmieniono przypisanie.")
            print(f"przypisany_uzytkownik_id: {res.get('przypisany_uzytkownik_id')}")
        self.pause()

    # ---------- menu ----------
    def menu_glowne(self):
        while True:
            self.clear_screen()
            self.header("CLI TESTOWE - TASK MANAGER")

            print("\n1) Zarejestruj")
            print("2) Zaloguj")
            print("3) Wyjdź")

            w = input("\nWybór: ").strip()
            if w == "1":
                self.clear_screen()
                self.rejestracja()
            elif w == "2":
                self.clear_screen()
                if self.logowanie():
                    self.menu_zalogowany()
            elif w == "3":
                print("\nDo widzenia, Panie.")
                return
            else:
                print("❌ Zły wybór.")
                self.pause()

    def menu_zalogowany(self):
        while True:
            self.clear_screen()
            self.header(f"ZALOGOWANY | user_id={self.uzytkownik_id}")

            print("\n--- SINGLE ---")
            print("1) Single: listy (pokaż)")
            print("2) Single: listy (dodaj)")
            print("3) Single: listy (usuń)")
            print("4) Single: zadania w liście (pokaż)")
            print("5) Single: zadanie (dodaj)")
            print("6) Single: zadanie (usuń)")
            print("7) Single: zadanie (zmień stan)")

            print("\n--- MULTI (wszyscy równi) ---")
            print("8)  Multi: listy (pokaż)")
            print("9)  Multi: listy (utwórz)")
            print("10) Multi: członkowie (pokaż)")
            print("11) Multi: członek (dodaj po loginie)")
            print("12) Multi: członek (usuń po user_id)")
            print("13) Multi: zadania w liście (pokaż)")
            print("14) Multi: zadanie (dodaj)")
            print("15) Multi: zadanie (edytuj)")
            print("16) Multi: zadanie (usuń)")
            print("17) Multi: zadanie (zmień stan)")
            print("18) Multi: zadanie (przypisz/odpisz)")

            print("\n--- ---")
            print("19) Wyloguj")

            w = input("\nWybór: ").strip()

            if w == "1":
                self.s_listy()
            elif w == "2":
                self.s_listy_dodaj()
            elif w == "3":
                self.s_listy_usun()
            elif w == "4":
                self.s_zadania_w_liscie()
            elif w == "5":
                self.s_zadanie_dodaj()
            elif w == "6":
                self.s_zadanie_usun()
            elif w == "7":
                self.s_zadanie_stan()

            elif w == "8":
                self.m_listy()
            elif w == "9":
                self.m_listy_dodaj()
            elif w == "10":
                self.m_members()
            elif w == "11":
                self.m_member_add()
            elif w == "12":
                self.m_member_remove()
            elif w == "13":
                self.m_zadania_w_liscie()
            elif w == "14":
                self.m_zadanie_dodaj()
            elif w == "15":
                self.m_zadanie_edytuj()
            elif w == "16":
                self.m_zadanie_usun()
            elif w == "17":
                self.m_zadanie_stan()
            elif w == "18":
                self.m_zadanie_przypisz()

            elif w == "19":
                if input("\nPotwierdź wylogowanie (tak/nie): ").strip().lower() == "tak":
                    self.clear_token()
                    print("✅ Wylogowano.")
                    self.pause()
                    return
            else:
                print("❌ Zły wybór.")
                self.pause()

    def run(self):
        # Jeśli token zapisany, nie zakładam że jest ważny — ale zwykle ułatwia.
        if self.token_csrf and self.uzytkownik_id:
            self.menu_zalogowany()
        else:
            self.menu_glowne()


def main():
    TaskManagerCLI().run()


if __name__ == "__main__":
    main()