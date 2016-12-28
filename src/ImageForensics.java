import java.util.concurrent.Semaphore;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;


public class ImageForensics 
{
	static Mat image ;
	static double[][] dctArray ;
	int blockSize = 3;
	//static Mat image_gray ;

	static Semaphore ifLock = new Semaphore(0);
	
	public ImageForensics(String file) 
	{
		image = Highgui.imread(file);
	}

	public void run()
	{
		try
		{
			Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
			//Mat image = Highgui.imread("fruits.jpg");

			int row = image.rows();
			int col = image.cols();

			dctArray = new double[(row-blockSize+1) * (col-blockSize+1)][blockSize*blockSize];
			
			System.out.println("Print block ... ");
			
			
			int count = 0;
			for(int i=0;i<image.rows()-blockSize;i++)
			{
				for(int j=0;j<image.cols()-blockSize;j++)
				{
					double[] temp = calculateDCT(createBlock(i,j));
					for(int k=0;k<temp.length;k++)
					{
						dctArray[count][k] = temp[k];
					}
				}
				count++;
			}
			
			System.out.println("FINISHED");
			ifLock.release();
		}
		catch(NullPointerException ex)
		{
			System.out.println("Image not found");
		}
	}


	// creating blocks from the image
	public double[][] createBlock(int x, int y)
	{
		double[][] block = new double[blockSize][blockSize];

		for(int i=x, m=0;i<blockSize+x;i++,m++)
		{
			for(int j=y,n=0;j<blockSize+y;j++,n++)
			{
				double [] temp = image.get(x, y);
				block[m][n] =  temp[0];
			}
		}
		return block;
	}


	
	public double[] calculateDCT(double[][] block)
	{
		int N = blockSize;
		double retArray[] = new double[blockSize * blockSize];
		double[] c = new double[N];

		for (int i=1;i<N;i++) 
		{
			c[i]=1;
		}
		c[0]=1/Math.sqrt(2.0);

		double[][] F = new double[N][N];
		for (int u=0;u<N;u++)
		{
			for (int v=0;v<N;v++) 
			{
				double sum = 0.0;
				for (int i=0;i<N;i++) 
				{
					for (int j=0;j<N;j++) 
					{
						sum+=Math.cos(((2*i+1)/(2.0*N))*u*Math.PI)*Math.cos(((2*j+1)/(2.0*N))*v*Math.PI)*block[i][j];
					}
				}
				
				sum*=((c[u]*c[v])/4.0);
				F[u][v]=sum;
			}
		}

		int count =0;
		
		for(int m=0;m<F.length;m++)
		{
			for(int n=0;n<F[m].length;n++)
			{
				//System.out.println(F[m][n]);
				retArray[count++] =  F[m][n];
			}
		}

		return retArray;
	}

	public static void main(String[] args) 
	{
		System.loadLibrary("opencv_java249");

		//		System.out.println("Enter image file: ");
		//		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		//		String file = br.readLine();
		
		new ImageForensics("fruits.jpg").run();
		
		try {
			ifLock.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for(int i=0; i < dctArray.length; i++)
		{
			for(int j=0; j < dctArray[0].length; j++)
			{
			//	System.out.print(dctArray[i][j] + " ");
			}
			//System.out.println();
		}
		
	}

}
