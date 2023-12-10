import json
import os
from json import JSONDecodeError
from pathlib import Path
from typing import List

import openai
from dotenv import load_dotenv
from tqdm import tqdm

load_dotenv()


def generate_phrases(system: str, prompt: str):
    response = openai.chat.completions.create(
        model="gpt-4-turbo",
        messages=[
            {"role": "system", "content": system},
            {"role": "user", "content": prompt},
        ],
        temperature=0.8,
        top_p=1,
        max_tokens=200,
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


def to_stem(key: str):
    return key.split("/")[0]


def unflatten_layout(layout: List[str]):
    keys = {}
    for key in layout:
        if key is None:
            continue
        stem = to_stem(key)
        if stem not in keys:
            keys[stem] = []
        keys[stem].append(key)
    return keys


layout = read_file(get_lang_path())
grouped_layout = unflatten_layout(layout)

default_lang = load_json(get_lang_path())

valid_phrases = ["dialogue."]

system_prompt = (
    "Generate responses for a Minecraft villager, speaking and behaving like a human. You are presented with "
    "a set of base phrases and should generate new phrases with similar meaning, but a different "
    "personality. Be creative, and a bit exaggerating when needed."
)


def get_prompt(key: str, personality: str):
    return f"Generate similar phrases with similar length and exactly the same amount for the group `{key}`, but with a {personality} personality:"


def valid_phrase(key: str):
    for phrase in valid_phrases:
        if key.startswith(phrase) and "/" in key:
            return True
    return False


personalities = ["grumpy"]


def process(personality: str):
    path = get_lang_path("mca_dialogue_" + personality)

    existing_lang = load_json(path)

    os.makedirs(Path(path).parent, exist_ok=True)

    with open(path, "w") as file:
        file.write("{\n")
        newline_required = False
        processed_groups = set()
        for key in tqdm(layout, f"Processing {personality}"):
            if key is None:
                if newline_required:
                    file.write("\n")
                    newline_required = False
            else:
                if valid_phrase(key):
                    if key in existing_lang:
                        file.write(f'  "{key}": "{existing_lang[key]}",\n')
                    else:
                        stem = to_stem(key)
                        if stem not in processed_groups:
                            processed_groups.add(stem)
                            phrases = []
                            i = 0
                            for grouped_key in grouped_layout[stem]:
                                if grouped_key not in existing_lang:
                                    i += 1
                                    phrases.append(f"{i}: {default_lang[grouped_key]}")

                            generated_text = generate_phrases(
                                system_prompt,
                                get_prompt(key, personality)
                                + "\n"
                                + "\n".join(phrases),
                            )
                            print("Requested generation.")

                            try:
                                generated_phrases = [
                                    (
                                        phrase.strip().split(":")[1]
                                        if ":" in phrase
                                        else phrase.strip()
                                    )
                                    for phrase in generated_text.split("\n")
                                ][-len(phrases) :]

                                assert len(generated_phrases) == len(phrases)

                                for i, grouped_key in enumerate(grouped_layout[stem]):
                                    file.write(
                                        f'  "{grouped_key}": "{generated_phrases[i]}",\n'
                                    )
                            except Exception as e:
                                print(e)
                    newline_required = True
        file.write('"_": ""\n')
        file.write("}")


def main():
    for personality in personalities:
        process(personality)


if __name__ == "__main__":
    main()
