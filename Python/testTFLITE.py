import numpy as np
from preprocessors.imagetoarraypreprocessor import ImageToArrayPreprocessor
import time
import argparse
import cv2
from scripts import Extractor
from scripts import Retrievor
from preprocessors import AspectAwarePreprocessor
import time
from collections import Counter
import glob
import os
import progressbar
import pickle
from collections import OrderedDict

np.set_printoptions(threshold=np.inf)

types = [ #neural networks
    ('MobileNetV3_Large_100', '../models/src/main/assets/lite-model_imagenet_mobilenet_v3_large_100_224_classification_5_default_1.tflite'), 
    ('MobileNetV3_Large_075', '../models/src/main/assets/lite-model_imagenet_mobilenet_v3_large_075_224_classification_5_default_1.tflite'),
    ('MobileNetV3_Small_100', '../models/src/main/assets/lite-model_imagenet_mobilenet_v3_small_100_224_classification_5_default_1.tflite')
]

DEPTH = 4
MAX_DISTANCE = 100*100

ap = argparse.ArgumentParser()
ap.add_argument('-i', '--images', help='path of the test images')
args = vars(ap.parse_args())

# LOAD IMAGE PATHS
if args['images']:
    dataImages = glob.glob(args['images']+"*/*/*.jpg")
else:
    if not os.path.isfile('features/test_set.pck'):
            raise ValueError("File of test set doesn't exist. Run build_sqlite.py with ALL_DATASET set to False and try again")
    else:
        with open('features/test_set.pck', 'rb') as fp:
                dataImages = pickle.load(fp)
dataset = list()

#Progress bar
widgets = [
    "Loading train dataset ...", progressbar.Percentage(), " ",
    progressbar.Bar(), " ", progressbar.ETA()
]
pbar = progressbar.ProgressBar(
    maxval=len(dataImages), widgets=widgets
).start()


for (i, path) in enumerate(dataImages):
    pathSplitted = path.split(os.path.sep)[-2].split('_')
    monument = pathSplitted[0]+" "+pathSplitted[1]
    tupleImage = (monument,path)
    dataset.append(tupleImage)
    pbar.update(i)
pbar.finish()




# Initialize process
aap = AspectAwarePreprocessor(224, 224)
iap = ImageToArrayPreprocessor()

# loop over images & models
for dType,model_path in types:
    #print('[MODEL]: '+ dType)
    print('[INFO]: Working with {} ...'.format(dType))
    extractor = Extractor(dType,model_path)
    retrievor = Retrievor('features/'+dType+'_features.pck')

    #Progress bar
    widgets = [
        "Evaluating model ...", progressbar.Percentage(), " ",
        progressbar.Bar(), " ", progressbar.ETA()
    ]
    pbar = progressbar.ProgressBar(
        maxval=len(dataset), widgets=widgets
    ).start()

    #Initialize results

    correct = 0
    wrong = 0
    correctSecond = 0
    correctThird = 0
    notRecognized = 0

    preprocessTime = 0
    inferenceTime = 0
    faissTime = 0
    postprocessTime = 0
    

    for i,(correctMonument,path) in enumerate(dataset):

        image = cv2.imread(path)
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

        # PREPROCESSING

        startPreprocess = time.time()
        
        image,image1,image2 = aap.preprocessAugumentation(image,0.5,0.3)

        image = iap.preprocess(image)
        image1 = iap.preprocess(image1)
        image2 = iap.preprocess(image2)

        endPreprocess = time.time()

        preprocessTime += endPreprocess - startPreprocess

        # INFERENCE

        startRecognize = time.time()

        features = extractor.extract(image)
        features1 = extractor.extract(image1)
        features2 = extractor.extract(image2)

        endRecognize = time.time()

        inferenceTime += endRecognize - startRecognize

        # FAISS

        startFaiss = time.time()

        result = retrievor.searchFAISS2(features, depth=DEPTH, distance='euclidean')
        result1 = retrievor.searchFAISS2(features1, depth=DEPTH, distance='euclidean')
        result2 = retrievor.searchFAISS2(features2, depth=DEPTH, distance='euclidean')

        endFaiss = time.time()

        faissTime += endFaiss - startFaiss

        # POST PROCESS

        startPostProcess = time.time()

        deleted = 0

        for j,value in enumerate(result[1][0]):
            if value > MAX_DISTANCE:
                result[0][0].pop(j-deleted)
                deleted += 1

        deleted = 0

        for j,value in enumerate(result1[1][0]):
            if value > MAX_DISTANCE:
                result1[0][0].pop(j-deleted)
                deleted += 1


        deleted = 0

        for j,value in enumerate(result2[1][0]):
            if value > MAX_DISTANCE:
                result2[0][0].pop(j-deleted)
                deleted += 1
                

        d = Counter(result[0][0])
        d += Counter(result1[0][0])
        d += Counter(result2[0][0])

        sorted_dict = dict(sorted(d.items(),key=lambda item: item[1], reverse=True))

        keyList = list(sorted_dict.keys())
        valueList = list(sorted_dict.values())

    

        if len(sorted_dict) > 0:

            firstResult = keyList[0]

            if len(sorted_dict) > 1:
                firstValue = valueList[0]
                secondValue = valueList[1]
                secondResult = keyList[1]

                if len(sorted_dict) > 2:
                    thirdResult = keyList[2]
                    secondValue = valueList[2]

        else:
            firstResult = None

        endPostProcess = time.time()

        postprocessTime += endPostProcess - startPostProcess

        # CHECKING RESULTS

        if(str(firstResult) == correctMonument):
            correct += 1
        elif (len(sorted_dict) > 1 and str(secondResult) == correctMonument):
            if firstValue == secondValue:
                correct += 1
            else:
                correctSecond += 1
        elif (len(sorted_dict) > 2 and str(thirdResult) == correctMonument):
            if firstValue == thirdResult:
                correct += 1
            elif secondValue == thirdResult:
                correctSecond += 1
            else:
                correctThird += 1
        elif firstResult == None:
            notRecognized += 1
        else:
            wrong += 1

        pbar.update(i)
    pbar.finish()

    preprocessTimeAverage = preprocessTime / len(dataset)
    inferenceTimeAverage = inferenceTime / len(dataset)
    faissTimeAverage = faissTime / len(dataset)
    postprocessTimeAverage = postprocessTime / len(dataset)

    print("RESULT")
    print("Correct: ", correct)
    print("Correct at second: ", correctSecond)
    print("Correct at third: ", correctThird)
    print("Not recognized: ", notRecognized)
    print("Wrong: ", wrong)

    print("AVERAGE TIME")
    print("Preprocess: ", preprocessTimeAverage)
    print("Inference: ", inferenceTimeAverage)
    print("Faiss: ", faissTimeAverage)
    print("Postprocess: ", postprocessTimeAverage)



    