# necessary packages
from cmath import log
import os
import pickle
import numpy as np
import faiss
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.metrics.pairwise import manhattan_distances
from sklearn.metrics.pairwise import euclidean_distances


class Retrievor:
    def __init__(self, compressor):
        if not os.path.isfile(compressor):
            raise ValueError("File of features doesn't exist")
        self.__load_compressor(compressor)

    def __load_compressor(self, compressor):
        with open(compressor, 'rb') as fp:
            features = pickle.load(fp)
        name = [f[1] for f in features]
        matrix = [f[0] for f in features]
        self.matrix = np.array(matrix)
        self.names = np.array(name)
        self.faissIndex = None


    def compute_distance(self, vector, distance='euclidean'):
        v = vector.reshape(1, -1)
        if distance == 'cosinus':
            return cosine_similarity(self.matrix, v)
        elif distance == 'manhattan':
            return manhattan_distances(self.matrix, v)
        elif distance == 'euclidean':
            return euclidean_distances(self.matrix, v)

    def search(self, wanted, distance='euclidean', depth=1):
        distances = self.compute_distance(wanted, distance).flatten()
        nearest_ids = np.argsort(distances)[:depth].tolist()

        n = distances.shape[0]
        #print("Calculated distances: ",n)
        #print("Worst case n^2: ", n*n)
        #print("Best case n log n: ", n*log(n))
        print("Expected comparisons: ", 1.4*n*log(n))

        return [
            self.names[nearest_ids].tolist(),
            distances[nearest_ids].tolist()
        ]

    def searchFAISS(self, wanted, distance='euclidean', depth=1):

        wanted = np.array([wanted])

        #FAISS

        k = depth

        if(self.faissIndex == None):
            d = self.matrix.shape[1]
            self.faissIndex = faiss.IndexFlatL2(d)  # the other index
            self.faissIndex.add(self.matrix)                  # add may be a bit slower as well

        D, I = self.faissIndex.search(wanted, k)     # actual search


        return [
            self.names[I].tolist(),
            D
        ]

    def searchFAISS2(self, wanted, distance='euclidean', depth=1):

        wanted = np.array([wanted])

        #FAISS

        k = depth

        if(self.faissIndex == None):
            d = self.matrix.shape[1]
            nlist = 50
            quantizer = faiss.IndexFlatL2(d)  # the other index
            self.faissIndex = faiss.IndexIVFFlat(quantizer, d, nlist, faiss.METRIC_L2)

            self.faissIndex.train(self.matrix)
            self.faissIndex.add(self.matrix)                  # add may be a bit slower as well

        D, I = self.faissIndex.search(wanted, k)     # actual search
   

        return [
            self.names[I].tolist(),
            D
        ]