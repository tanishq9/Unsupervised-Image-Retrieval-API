import sys
import numpy as np
from PIL import Image
from numpy import asarray
from sklearn.neighbors import NearestNeighbors
from keras.models import load_model 
import matplotlib.pyplot as plt
import os

def get_K(path,K):
    # load the encoder
    encoder = load_model('encoder.h5')
    # load the test set
    X_test = np.load('test_data.npy')
    # load input image and change it to numpy array
    image = Image.open(path)
    query = asarray(image)[:,:,0].reshape(28,28)
    print(query.shape)
    # reshape
    X_test = X_test.reshape(10000,28,28,1)
    # encode the test image set and query
    codes = encoder.predict(X_test)
    query_code = encoder.predict(query.reshape(1,28,28,1))
    # fit knn to the test set 
    n_neigh = int(K)
    codes = codes.reshape(-1, 4*4*8)
    query_code = query_code.reshape(1, 4*4*8)
    nbrs = NearestNeighbors(n_neighbors=n_neigh).fit(codes)
    distances, indices = nbrs.kneighbors(np.array(query_code))  
    closest_images = X_test[indices]
    closest_images = closest_images.reshape(-1,28,28,1); print(closest_images.shape)
    # show k images
    plt.figure(figsize=(20, 10))    
    for i in range(n_neigh):
        # display original
        ax = plt.subplot(1, n_neigh, i+1)
        plt.imshow(closest_images[i].reshape(28, 28))
        im = Image.fromarray(closest_images[i].reshape(28, 28))
        filename = "output_image_"+str(i)+".png"
        im.save(os.path.abspath(os.getcwd())+"\\output_images\\"+filename)
        plt.gray()
        ax.get_xaxis().set_visible(False)
        ax.get_yaxis().set_visible(False)

if __name__ == "__main__":
   img_file = sys.argv[1]
   K = sys.argv[2]
   print("Inside Python!")
   get_K(img_file,K) 