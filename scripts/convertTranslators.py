import json
from datetime import datetime

if __name__ == '__main__':
    min_actions = 10
    members = json.load(open("./topMembers.json", "r", encoding="UTF-8"))
    ignored = 0

    replacements = {
        "ALEJANDRO MARCELO PAUCAR CASTILLO": "Alejandro Castillo",
        "Raffox97 (Community volunteer)": "Raffox97",
        "Betancourt Guerrero Ramón": "Betancourt Ramón",
        "Marco Antônio Gonçalves Aragão": "Marco Aragão",
        "Sittinan Sarapoke (Zenozuki)": "Sittinan Sarapoke",
        "Manuel Giménez González": "Manuel González",
        "juan marcelo casas gonzales": "Juan Gonzales",
    }

    for m in members["data"]:
        date = datetime.strptime(m["user"]["joined"], "%Y-%m-%d %H:%M:%S")
        if m["translated"] + m["approved"] + m["voted"] > min_actions and m["user"]["username"] != "Luke100000":
            name = m["user"]["fullName"].replace(" (" + m["user"]["username"] + ")", "")
            if name in replacements:
                name = replacements[name]
            print('"' + name + '",')
        else:
            ignored += 1

    print()
    print("filtered", ignored)
