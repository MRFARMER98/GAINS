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

def test_query(input_dict):
    gql = """
    query ProductSearch($input: ProductSearchInput!) {
      productSearch(input: $input) {
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
    """
    r = requests.post("https://api.ah.nl/graphql", headers=headers, json={"query": gql, "variables": {"input": input_dict}}, timeout=10)
    print("INPUT:", input_dict)
    print("STATUS:", r.status_code)
    print("BODY:", json.dumps(r.json(), indent=2)[:1000])

print("--- Test ProductSearchInput variations ---")
test_query({"query": "kwark", "page": 0, "size": 10})
test_query({"query": "bonus"})
