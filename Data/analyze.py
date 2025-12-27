import pandas as pd

def analyze(file):
    df = pd.read_csv(file)
    lat = df["latency_ns"]

    print(f"\n{file}")
    print("Mean (ns):", int(lat.mean()))
    print("Median (ns):", int(lat.median()))
    print("P95 (ns):", int(lat.quantile(0.95)))
    print("P99 (ns):", int(lat.quantile(0.99)))

analyze("set_latency.csv")
analyze("get_latency.csv")
