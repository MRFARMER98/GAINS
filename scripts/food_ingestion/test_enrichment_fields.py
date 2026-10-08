import requests
import json

res = requests.post(
    "https://api.ah.nl/mobile-auth/v1/auth/token/anonymous",
    json={"clientId": "appie"},
    headers={"User-Agent": "Appie/8.22.3"},
    timeout=10
)
token = res.json().get("access_token")
headers = {
    "User-Agent": "Appie/8.22.3",
    "Authorization": f"Bearer {token}",
    "X-Application-Name": "AHMobileMenu"
}

def test_query(gql):
    r = requests.post("https://api.ah.nl/graphql", headers=headers, json={"query": gql}, timeout=10)
    print("STATUS:", r.status_code)
    print("BODY:", json.dumps(r.json(), indent=2)[:2000])

print("--- Test Clean GraphQL Product fields ---")
test_query("""
query {
  productSearch(input: {query: "kwark", page: 0, size: 3}) {
    products {
      id
      title
      brand
      category
      salesUnitSize
      price {
        now {
          amount
        }
        was {
          amount
        }
      }
    }
  }
}
""")
