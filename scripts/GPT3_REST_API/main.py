import os

import openai
import patreon
from fastapi import FastAPI
from pyrate_limiter import Duration, Limiter, RequestRate, BucketFullException

app = FastAPI()

openai.api_key = os.getenv("OPENAI_API_KEY")

model = "text-curie-001"

limiter = Limiter(RequestRate(600, Duration.HOUR))
limiter_premium = Limiter(RequestRate(6000, Duration.HOUR))

LIMIT_EXCEEDED = "(You exceeded your hourly rate, give the AI some rest! Also make sure to use the newest version for best results!)"

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


@app.get("/chat")
def chat(prompt: str, player: str, villager: str):
    try:
        lim = limiter_premium if player in premium else limiter
        for i in range(len(prompt) // 100 + 1):
            lim.try_acquire(player)
        print(player, lim.get_current_volume(player), player in premium)

        response = openai.Completion.create(
            model=model,
            prompt=prompt,
            temperature=0.9,
            max_tokens=150,
            top_p=1,
            frequency_penalty=0.5,
            presence_penalty=0.0,
            stop=[f"{player}:", f"{villager}:"],
        )

        return {"answer": response["choices"][0]["text"]}
    except BucketFullException:
        if player in premium:
            return {"answer": LIMIT_EXCEEDED, "error": "limit_premium"}
        else:
            return {"answer": LIMIT_EXCEEDED, "error": "limit"}
