import requests

token_res = requests.post(
    'https://api.ah.nl/mobile-auth/v1/auth/token/anonymous',
    json={'clientId': 'appie'},
    headers={'User-Agent': 'Appie/8.22.3'}
)
print("Auth token status:", token_res.status_code)
token = token_res.json().get('access_token')

headers = {
    'User-Agent': 'Appie/8.22.3',
    'Authorization': f'Bearer {token}',
    'X-Application-Name': 'AHMobileMenu'
}

endpoints = [
    ("https://api.ah.nl/mobile-services/product/search/v2", {"query": "kwark"}),
    ("https://api.ah.nl/mobile-services/product/search/v1", {"query": "kwark"}),
    ("https://api.ah.nl/mobile-services/product/search", {"query": "kwark"}),
    ("https://api.ah.nl/mobile-services/v1/product/search", {"query": "kwark"}),
    ("https://api.ah.nl/mobile-services/v2/products/search", {"query": "kwark"}),
    ("https://api.ah.nl/mobile-services/v1/products/search", {"query": "kwark"}),
]

for url, params in endpoints:
    try:
        r = requests.get(url, headers=headers, params=params, timeout=5)
        print(f"GET {url} -> Status: {r.status_code}")
        if r.status_code == 200:
            print("  SUCCESS! Keys:", list(r.json().keys()) if isinstance(r.json(), dict) else "List")
    except Exception as e:
        print(f"GET {url} -> Exception: {e}")

# Test GraphQL
gql_query = {
    "query": """
    query ProductSearch($input: ProductSearchInput!) {
      productSearch(input: $input) {
        products {
          id
          title
          brand
          subTitle
          mainCategory
        }
      }
    }
    """,
    "variables": {
        "input": {
            "query": "kwark"
        }
    }
}
try:
    r = requests.post("https://api.ah.nl/graphql", headers=headers, json=gql_query, timeout=10)
    print(f"POST GraphQL https://api.ah.nl/graphql -> Status: {r.status_code}")
    print("  Body:", r.text[:600])
except Exception as e:
    print(f"POST GraphQL Exception: {e}")

