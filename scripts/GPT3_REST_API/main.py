import os

import openai
import patreon
from fastapi import FastAPI
from pyrate_limiter import Duration, Limiter, RequestRate, BucketFullException

app = FastAPI()

openai.api_key = os.getenv("OPENAI_API_KEY")

model = "text-curie-001"

limiter = Limiter(RequestRate(700, Duration.HOUR))
limiter_premium = Limiter(RequestRate(7000, Duration.HOUR))

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
            temperature=0.95,
            max_tokens=150,
            frequency_penalty=0.6,
            presence_penalty=0.6,
            stop=[f"{player}:", f"{villager}:"],
        )

        return {"answer": response["choices"][0]["text"]}
    except BucketFullException:
        if player in premium:
            return {"answer": LIMIT_EXCEEDED, "error": "limit_premium"}
        else:
            return {"answer": LIMIT_EXCEEDED, "error": "limit"}


from transformers import AutoModelForSequenceClassification
from transformers import AutoTokenizer, AutoConfig
from transformers import pipeline

SENTIMENT_MODEL = "cardiffnlp/twitter-xlm-roberta-base-sentiment"

tokenizer = AutoTokenizer.from_pretrained(SENTIMENT_MODEL)
config = AutoConfig.from_pretrained(SENTIMENT_MODEL)
sentiment_model = AutoModelForSequenceClassification.from_pretrained(SENTIMENT_MODEL)


def get_sentiment(text):
    encoded_input = tokenizer(text, return_tensors="pt")
    output = sentiment_model(**encoded_input)
    scores = output[0][0].detach().numpy()
    return scores[2] - scores[0]


@app.get("/sentiment")
def sentiment(prompt: str):
    return {"result": float(get_sentiment(prompt))}


ENABLE_CLASSIFIER = False

if ENABLE_CLASSIFIER:
    CLASSIFY_MODEL = "facebook/bart-large-mnli"
    classifier = pipeline("zero-shot-classification", CLASSIFY_MODEL)

    @app.get("/classify")
    def classify(prompt: str, classes: str):
        classes = [t.strip() for t in classes.split(",")]
        probabilities = classifier(prompt, classes, multi_label=True)

        results = {
            label: float(score)
            for (label, score) in zip(probabilities["labels"], probabilities["scores"])
        }
        return {"result": results}
