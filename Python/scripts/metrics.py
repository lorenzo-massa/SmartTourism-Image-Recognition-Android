# -*- coding: utf-8 -*-
import numpy as np


def mean_reciprocal_rank(retrievals, labels):
    rr = []
    for (retrieval, label) in zip(retrievals, labels):
        rank = 0
        if label in retrieval:
            rank = retrieval.index(label) + 1
        rr.append(rank / len(retrieval))
    return np.mean(rr)


def rank1_accuracy(retrievals, labels):
    first = []
    for (retrieval, label) in zip(retrievals, labels):
        if retrieval[0] == label:
            first.append(1)
        else:
            first.append(0)
    return np.mean(first)


def mean_mean_average_precision(retrievals, labels):
    precisions = []
    for (retrieval, label) in zip(retrievals, labels):
        precision, hit = [], 0
        for i, name in enumerate(retrieval):
            if name == label:
                hit += 1
                precision.append(hit / (i + 1))
        if hit == 0:
            precisions.append(0.)
        else:
            precisions.append(np.mean(precision))
    return np.mean(precisions)
