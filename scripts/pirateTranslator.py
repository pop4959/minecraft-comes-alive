import json
import os
import urllib.parse
import urllib.request

from tqdm import tqdm


def translate(s):
    r = urllib.request.urlopen("https://pirate.monkeyness.com/api/translate?english=" + urllib.parse.quote(s))
    return urllib.parse.unquote(r.read().decode('utf-8'))


def load_json(path):
    file = open(path, "r")
    d = file.read()
    file.close()
    return json.loads(d)


def save_json(path, d):
    file = open(path, "w")
    file.write(json.dumps(d, indent=4))
    file.close()


def translate_all(path):
    phrases = load_json("../common/src/main/resources/assets/" + path + "/lang/en_us.json")
    new_phrases = {}

    for i in tqdm(phrases):
        new_phrases[i] = translate(phrases[i])

    os.makedirs("translated", exist_ok=True)
    save_json("translated/" + path + ".json", new_phrases)


if __name__ == "__main__":
    translate_all("mca_books")
    translate_all("mca_dialogue")
    translate_all("mca")
