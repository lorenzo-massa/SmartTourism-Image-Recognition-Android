# necessary package
import os
import cv2
import pickle
import numpy as np
import progressbar

from ..utils.timer import Timer
from ..utils.utils import printProgressBar
from ..datasets.loadimages import LoadImages
from sklearn.metrics import mean_squared_error


class Evaluator(object):
    def __init__(self, distance, depth=10):
        if not isinstance(distance, str):
            raise ValueError('distance must be a string')
        if not isinstance(depth, int):
            raise ValueError('depth must be an integer')
        self.distance = distance
        self.depth = depth

    def evaluate(self, extractor, retrievor, data, labels):
        timer = Timer()
        precisions, rr, first = [], [], []
        length = len(data)

        printProgressBar(0, length, prefix='Progress:',
                         suffix='Complete', length=50)
        for (ids, image) in enumerate(data):
            # search
            timer.tic()
            feature = extractor.extract(image)
            if not isinstance(feature, np.ndarray):
                timer.toc()
                continue
            retrieval, _ = retrievor.search(
                feature,
                self.distance,
                self.depth
            )
            timer.toc()

            # precision and reciprocal rank
            # reciprocal rank and first rank
            rank = 0
            try:
                rank = retrieval.index(labels[ids]) + 1
                rr.append(rank / self.depth)
                if rank == 1:
                    first.append(1)
                else:
                    first.append(0)
            except ValueError as _:
                first.append(0)
                rr.append(0)
            # precision
            if rank > 0:
                precision, hit = [], 0
                for i, name in enumerate(retrieval):
                    if name == labels[ids]:
                        hit += 1
                        precision.append(hit/(i+1))
                if hit == 0:
                    precisions.append(0.)
                else:
                    precisions.append(np.mean(precision))

            printProgressBar(ids + 1, length, prefix='Progress:',
                             suffix='Complete', length=50)
        # show result
        self.summary(
            np.mean(rr),
            np.mean(precisions),
            np.mean(first),
            timer.average_time
        )

    def summary(self, mrr, mmap, rank, timer_avgs):
        print('mean reciprocal rank: {}'.format(mrr))
        print('mean of mean average precision: {}'.format(mmap))
        print('rank #1 accuracy: {}'.format(rank))
        print('average time per query: {}'.format(timer_avgs))
