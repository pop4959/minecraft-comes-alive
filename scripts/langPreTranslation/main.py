import json
import os
from json import JSONDecodeError
from pathlib import Path

import openai
from dotenv import load_dotenv

load_dotenv()


def generate_phrases(system: str, prompt: str):
    response = openai.chat.completions.create(
        model="gpt-3.5-turbo",
        messages=[
            {"role": "system", "content": system},
            {"role": "user", "content": prompt},
        ],
        temperature=0.8,
        top_p=1,
        max_tokens=100,
    )
    return response.choices[0].message.content


def read_file(file_path: str):
    keys = []
    if os.path.exists(file_path):
        with open(file_path) as file:
            for line in file.readlines():
                if line.isspace():
                    keys.append(None)
                else:
                    keys.append(line.split(":")[0].strip()[1:-1])
    return keys


def get_lang_path(namespace: str = "mca_dialogue"):
    return f"../../common/src/main/resources/assets/{namespace}/lang/en_us.json"


def load_json(file_path: str):
    if os.path.exists(file_path):
        try:
            with open(file_path) as file:
                return json.load(file)
        except JSONDecodeError:
            return {}
    return {}


layout = read_file(get_lang_path())

default_lang = load_json(get_lang_path())

valid_phrases = ["dialogue."]

system_prompt = (
    "Generate responses for a Minecraft villager, speaking and behaving like a human. You are presented with "
    "a set of base phrases and should generate new phrases with similar meaning, but a different "
    "personality. Be creative, and a bit exaggerating when needed."
)


def get_prompt(key: str, personality: str):
    return f"Generate a similar phrase for the group `{key}`, but with a {personality} personality:"


def valid_phrase(key: str):
    for phrase in valid_phrases:
        if key.startswith(phrase):
            return True
    return False


personalities = ["grumpy"]

max_gens = 5


def process(personality: str):
    global max_gens
    path = get_lang_path("mca_dialogue_" + personality)

    existing_lang = load_json(path)

    os.makedirs(Path(path).parent, exist_ok=True)

    with open(path, "w") as file:
        file.write("{\n")
        newline_required = False
        for key in layout:
            if key is None:
                if newline_required:
                    file.write("\n\n")
                    newline_required = False
            else:
                if valid_phrase(key):
                    if key in existing_lang:
                        file.write(f'  "{key}": "{existing_lang[key]}",\n')
                    elif max_gens > 0:
                        max_gens -= 1
                        generated_text = generate_phrases(
                            system_prompt,
                            get_prompt(key, personality) + "\n" + default_lang[key],
                        )
                        file.write(f'  "{key}": "{generated_text}",\n')
                    newline_required = True
        file.write('"_": ""\n')
        file.write("}")


def main():
    for personality in personalities:
        process(personality)


if __name__ == "__main__":
    main()
