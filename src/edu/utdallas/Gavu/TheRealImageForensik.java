package edu.utdallas.Gavu;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.lang3.ArrayUtils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;

public class TheRealImageForensik 
{
	Mat srcImgMat;
	Mat srcGrayScaleMat;
	Mat dctMat;
	int blockSize = 6;
	double dctBlocksArr[][];
	int pixelsAboveThreshold[][];
	
	int shiftVectorC[][];
	final int SHIFT_THRESHOLD = 2;

	public TheRealImageForensik(Mat img)
	{
		srcGrayScaleMat = img;
		
		img.convertTo(img, CvType.CV_64FC1);

		log(img.rows() + " x " + img.cols());

		if(img.cols() % 2 != 0 || img.rows() % 2 != 0)
		{
			log("Sorry input an even image size");
			return;
		}

		pixelsAboveThreshold = new int[srcGrayScaleMat.rows()][srcGrayScaleMat.cols()];
		
		/* padding to make size multiple of 2 .. BC
		int m = Core.getOptimalDFTSize(img.rows());
	    int n = Core.getOptimalDFTSize(img.cols()); // on the border add zero values

	    Mat padded = new Mat(new Size(n, m), CvType.CV_64FC1); // expand input image to optimal size

		Imgproc.copyMakeBorder(secondImage, padded, 0, m - secondImage.rows(), 0, n - secondImage.cols(), Imgproc.BORDER_CONSTANT);
		 */

		dctMat = new Mat(srcGrayScaleMat.size(), CvType.CV_64FC1);

		log("Calculating DCT");
		//Core.dct(srcGrayScaleMat, dctMat);
		//log(dctMat.dump());

		
		

		int blockCnt = 0;
		int dctBlocksToArrCnt = 0;
		double dctBlocksArr_[][] = new double[srcGrayScaleMat.rows() * srcGrayScaleMat.cols()][(blockSize * blockSize) + 2];
		for(int i = 0; i < (srcGrayScaleMat.rows() - blockSize + 1); i++)
		{
			for(int j = 0; j < (srcGrayScaleMat.cols() - blockSize + 1); j++)
			{	
				Mat dctSrcBlock = new Mat(new Size(blockSize, blockSize), CvType.CV_64FC1);
				Mat dctDstBlock = new Mat(new Size(blockSize, blockSize), CvType.CV_64FC1);

				for(int br = i, brr = 0; br < (i + blockSize); br++, brr++)
				{
					for(int bc = j, bcc = 0; bc < (j + blockSize); bc++, bcc++)
					{
						dctSrcBlock.put(brr, bcc, srcGrayScaleMat.get(br, bc));
					}
				}

				// dct of block
				Core.dct(dctSrcBlock, dctDstBlock);
				//log("DCT done for Block for ["+i+"]["+j+"]");

				String str = "";
				int c = 0;
				
				for(int p = 0; p < dctDstBlock.rows(); p++)
				{
					for(int q = 0; q < dctDstBlock.cols(); q++)
					{
						dctBlocksArr_[dctBlocksToArrCnt][c] = dctDstBlock.get(p, q)[0];
						
						//log(dctDstBlock.get(p,q).length + "");
						str += " ["+dctBlocksToArrCnt+"]["+c+"] ";// + dctBlocksArr[cnt][c];
						c++;
					}
				}
				dctBlocksArr_[dctBlocksToArrCnt][c] = i; // storing pixel position
				c++;
				dctBlocksArr_[dctBlocksToArrCnt][c] = j; // storing pixel position
				
				blockCnt++;
				dctBlocksToArrCnt++;
				
			}// j
			
			dctBlocksToArrCnt++; //= blockSize;
		}// i
		
		
		dctBlocksArr = ArrayUtils.subarray(dctBlocksArr_, 0, dctBlocksToArrCnt - 1);
		
		/*for(double[] term: dctBlocksArr){
			System.out.println(Arrays.toString(term));
		}*/
		

		log("DCT Performed for all blocks: " + blockCnt);


		log("Lexicographically sorting ... ");

		Arrays.sort(dctBlocksArr, new Comparator<double[]>()
			{
				public int compare(double[] a, double[] b)
				{
					//assumes array length is 2
					double x, y;
					if(a[0] != b[0]) 
					{
						x = a[0];
						y = b[0];
						
						if (x < y) 
							return -1;
						else if (x == y) 
							return 0;
						else 
							return 1;
					}
					else
					{
						if(a.length > 2)
							return compare(ArrayUtils.subarray(a, 1, a.length - 1), ArrayUtils.subarray(b, 1, b.length - 1));
						else
							return 0;
					}
				}
			}); // sort(..) end

		/*
		for(double[] term: dctBlocksArr){
			System.out.println(Arrays.toString(term));
		}
		*/
		
		log("Sorting done!");
		
		
		log("Finding shift vectors ... ");
		
		shiftVectorC = new int[srcGrayScaleMat.rows()][srcGrayScaleMat.cols()];
		for(int x = 0; x < srcGrayScaleMat.rows(); x++)
		{
			for(int y = 0; y < srcGrayScaleMat.cols(); y++)
			{
				shiftVectorC[x][y] = 0;
				pixelsAboveThreshold[x][y] = 0;
			}
		}
		
		double prevDCTBlock[] = ArrayUtils.subarray(dctBlocksArr[0], 0, dctBlocksArr[0].length - 3);
		int prevR = (int)dctBlocksArr[0][dctBlocksArr[0].length - 2];
		int prevC = (int)dctBlocksArr[0][dctBlocksArr[0].length - 1];
		
		for(int p = 1; p < dctBlocksArr.length; p++)
		{
			double currDCTBlock[] = ArrayUtils.subarray(dctBlocksArr[p], 0, dctBlocksArr[p].length - 3);
			
			int r = (int)dctBlocksArr[p][dctBlocksArr[p].length - 2];
			int c = (int)dctBlocksArr[p][dctBlocksArr[p].length - 1];
			
			int shiftR = 0;
			int shiftC = 0;
					
			if(Arrays.equals(currDCTBlock, prevDCTBlock))
			{
				
				shiftR = Math.abs(prevR - r);
				shiftC = Math.abs(prevC - c);
				
				//log("pixel at : ("+r+","+c+") ... ["+shiftR+"]["+shiftC+"] << img("+r+","+c+") ... prevPixelAt: (" + prevR +","+ prevC + ")");
				
				shiftVectorC[shiftR][shiftC]++;
				
				if(shiftVectorC[shiftR][shiftC] > SHIFT_THRESHOLD)
				{
					log("["+shiftR+"]["+shiftC+"] = " + shiftVectorC[shiftR][shiftC]);
					pixelsAboveThreshold[r][c] = 1;
				}
				
			}
			
			prevDCTBlock = currDCTBlock;
			prevR = r;
			prevC = c;
		}
		/*
		for(int[] term: shiftVectorC){
			System.out.println(Arrays.toString(term));
		}
		*/
		
		
	}





	public static void log(String str)
	{
		System.out.println("ImageForeignSicks: " + str);
	}

	public static void main(String args[])
	{
		System.loadLibrary("opencv_java249");

		//String path = "c:\\circle.jpg";
		String path = "c:\\fruits.jpg";

		log("Loading IMREAD: " + path);
		new TheRealImageForensik(Highgui.imread(path, Highgui.CV_LOAD_IMAGE_GRAYSCALE));
		
	}

}
