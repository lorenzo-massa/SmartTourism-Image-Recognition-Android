import albumentations as A
import cv2
import matplotlib.pyplot as plt
from preprocessors import AspectAwarePreprocessor

transform = A.Compose([
    A.RandomBrightnessContrast(p=0.3),
    A.Perspective(p=1, scale=(0.1, 0.2), interpolation=cv2.INTER_NEAREST, pad_mode=cv2.BORDER_WRAP)

])

def augment(path, save = True):
    image = cv2.imread(path)
    transformed = transform(image=image)

    if save:
        pathSplitted = path.split(".")
        extension = pathSplitted[-1]
        newpath = pathSplitted[0]+"_aug"+"."+extension
        cv2.imwrite(newpath, transformed['image'])
        return newpath
    else:
        return transformed['image']

def test():
    image = cv2.imread("Python\datasetImages\Battistero_SanGiovanni\Battistero_SanGiovanni_002.jpg")
    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

    transformed1 = transform(image=image)
    transformed2 = transform(image=image)
    transformed3 = transform(image=image)

    fig = plt.figure(figsize=(20, 7))
    rows = 1
    columns = 4

    fig.add_subplot(rows, columns, 1)
    plt.imshow(image)
    plt.axis('off')
    plt.title("Original")

    fig.add_subplot(rows, columns, 2)
    plt.imshow(transformed1['image'])
    plt.axis('off')
    plt.title("Tranformed")

    fig.add_subplot(rows, columns, 3)
    plt.imshow(transformed2['image'])
    plt.axis('off')
    plt.title("Tranformed")

    fig.add_subplot(rows, columns, 4)
    plt.imshow(transformed3['image'])
    plt.axis('off')
    plt.title("Tranformed")

    plt.show()


if __name__ == "__main__":
    test()