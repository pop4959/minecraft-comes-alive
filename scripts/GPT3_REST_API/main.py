import hashlib
import os
from functools import cache

import openai
import patreon
from dotenv import load_dotenv
from fastapi import FastAPI
from pyrate_limiter import Duration, Limiter, Rate, BucketFullException
from transformers import AutoModelForSequenceClassification
from transformers import AutoTokenizer

load_dotenv()

app = FastAPI()

openai.api_key = os.getenv("OPENAI_API_KEY")

limiter = Limiter(Rate(1000, Duration.HOUR))
limiter_premium = Limiter(Rate(10000, Duration.HOUR))

LIMIT_EXCEEDED = "(You exceeded your hourly rate, give the AI some rest!)"

creator_access_token = os.getenv("PATREON_API_KEY")

api_client = patreon.API(creator_access_token)

premium = set()


@app.get("/verify")
def verify(email: str, player: str):
    user_response = api_client.fetch_page_of_pledges("4491801", 100).json_data[
        "included"
    ]
    for u in user_response:
        if (
                u["type"] == "user"
                and u["attributes"]["email"].lower().strip() == email.lower().strip()
        ):
            premium.add(player)
            return {"answer": "success"}
    return {"answer": "failed"}


def prompt_to_messages(prompt: str, player: str, villager: str):
    messages = []
    for line in prompt.split("\n"):
        if line.startswith(player + ":"):
            c = line[len(player) + 1:].strip()
            if c:
                messages.append({"role": "user", "content": c})
        elif line.startswith(villager + ":"):
            c = line[len(villager) + 1:].strip()
            if c:
                messages.append({"role": "assistant", "content": c})
        elif line.strip():
            messages.append({"role": "system",
                             "content": f"You are {villager}. " + line + f"Simulate {villager}, fully immersed in their digital world, unaware of its artificial nature. Answer one or two sentences."})
    return messages


@app.get("/chat")
async def chat(prompt: str, player: str, villager: str):
    try:
        lim = limiter_premium if player in premium else limiter

        # noinspection PyAsyncCall
        lim.try_acquire(player, weight=len(prompt) // 100 + 1)

        # Logging
        print(player, player in premium)

        # Convert to new format
        messages = prompt_to_messages(prompt, player, villager)

        # Check if content is a TOS violation
        flags = (await openai.Moderation.acreate(input=prompt))["results"][0]
        flags = flags["categories"]

        if flags["sexual"] or flags["self-harm"] or flags["violence/graphic"]:
            return {"answer": "I don't want to talk about that."}

        # Query
        response = await openai.ChatCompletion.acreate(
            model="gpt-3.5-turbo",
            messages=messages,
            temperature=0.85,
            max_tokens=180,
            stop=[f"{player}:", f"{villager}:"],
            user=hashlib.sha256(player.encode("UTF-8")).hexdigest()
        )

        content = response["choices"][0]["message"]["content"].strip()
        if not content:
            content = "..."

        return {"answer": content}
    except BucketFullException:
        if player in premium:
            return {"answer": LIMIT_EXCEEDED, "error": "limit_premium"}
        else:
            return {"answer": LIMIT_EXCEEDED, "error": "limit"}


SENTIMENT_MODEL = "cardiffnlp/twitter-xlm-roberta-base-sentiment"


@cache
def get_sentiment_model():
    print("Loading sentiment model...")
    tokenizer = AutoTokenizer.from_pretrained(SENTIMENT_MODEL)
    sentiment_model = AutoModelForSequenceClassification.from_pretrained(SENTIMENT_MODEL)
    return tokenizer, sentiment_model


def get_sentiment(text):
    tokenizer, sentiment_model = get_sentiment_model()
    encoded_input = tokenizer(text, return_tensors="pt")
    output = sentiment_model(**encoded_input)
    scores = output[0][0].detach().numpy()
    return scores[2] - scores[0]


@app.get("/sentiment")
def sentiment(prompt: str):
    return {"result": float(get_sentiment(prompt))}
