import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

def plot(file, title):
    df = pd.read_csv(file)
    lat = np.sort(df["latency_ns"] / 1000.0)  # µs
    y = np.arange(len(lat)) / len(lat)

    plt.figure()
    plt.plot(lat, y)
    plt.xlabel("Latency (µs)")
    plt.ylabel("CDF")
    plt.title(title)
    plt.grid(True)
    plt.show()

plot("set_latency.csv", "SET Latency CDF")
plot("get_latency.csv", "GET Latency CDF")
