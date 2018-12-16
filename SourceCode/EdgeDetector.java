import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Scanner;
import javax.imageio.ImageIO;
public class EdgeDetector {
public static void main(String[] args){
int crop_range = 3;
int THRESHOLD = 70;
try{
String path;
Scanner sc = new Scanner(System.in);
System.out.println("Image name: "); 
path = sc.nextLine();
System.out.println("Threshold: ");
String input = sc.nextLine(); 
THRESHOLD = Integer.parseInt( input );
sc.close();	

long start = System.currentTimeMillis();//get time at the start 
File input_image = new File("Source/"+path);	//input file
BufferedImage img = ImageIO.read(input_image);
int img_x = img.getWidth();		//image x size
int img_y = img.getHeight();	//image y size
int col;
//*************************************Grayscaling*************************************
int a,r,g,b,avg;
for(int y = 0;y<img_y;y++){
	for(int x = 0;x<img_x;x++){
		col = img.getRGB(x,y);		
		a = (col>>24)&0xff;		
		r = (col>>16)&0xff;		
		g = (col>>8)&0xff;		
		b = col&0xff;			
		avg = (r+g+b)/3;		
		col=(a<<24)|(avg<<16)|(avg<<8)|avg;	
		img.setRGB(x, y, col);				
	}
}
int kernelDivider = 159;
int[][] blur_kernel = new int[][] 
	{{2,4,5,4,2},
	{4,9,12,9,4},
    {5,12,15,12,5},
	{4,9,12,9,4},
	{2,4,5,4,2}};

//**************************************Blurring**************************************
	//creating new buffredimage to store blured image
BufferedImage blurred_img = new BufferedImage(img_x, img_y,BufferedImage.TYPE_INT_ARGB);

for(int y = (blur_kernel.length-1)/2;y<img_y-(blur_kernel.length-1)/2;y++){
	//going through every pixel of image exept edge(size of the kernel)
	for(int x = (blur_kernel[0].length-1)/2;x<img_x-(blur_kernel[0].length-1)/2;x++){

int neighborhood = 0;//int to store sum of all pixel inside kernel

int kern_y=0;
//going through kernel araund picked pixel
for (int bl_y = y-((blur_kernel.length-1)/2);bl_y<=y+((blur_kernel.length-1)/2);bl_y++){
	int kern_x=0;
	for (int bl_x = x-((blur_kernel[0].length-1)/2);bl_x<=x+((blur_kernel[0].length-1)/2);bl_x++){
		//incrementing neighborhood by the "weight" of pixel multiplied by kernel value
		neighborhood+=((img.getRGB(bl_x,bl_y)&0xff)*blur_kernel[kern_y][kern_x]);
		kern_x++;
	}
	kern_y++;
}
neighborhood=neighborhood/kernelDivider;
//now we're using neighborhood to store ARGB value after blurring 
neighborhood=(255<<24)|(neighborhood<<16)|(neighborhood<<8)|neighborhood;

blurred_img.setRGB(x,y,neighborhood);
	}
}

//********************************Sobel Edge Detection********************************
int[][] angle = new int[img_y][img_x];//array of edge directions (for Canny edge detector)
int[][] x_sobel_kernel = new int[][]{{-1,0,1},{-2,0,2},{-1,0,1}};//horizontal sobel KERNEL
int[][] y_sobel_kernel = new int[][]{{-1,-2,-1},{0,0,0},{1,2,1}};//vertical sobel KERNEL

//going through every pixel of image exept edge(size of the kernel)
for(int y = (x_sobel_kernel.length-1)/2;y<img_y-(x_sobel_kernel.length-1)/2;y++){
	for(int x = (x_sobel_kernel[0].length-1)/2;x<img_x-(x_sobel_kernel[0].length-1)/2;x++){
		int neighbor_x = 0;	
		int kern_y=0;
		//iterating pixels around picked pixel according to x kernel (finding horizontal edges)
		for (int bl_y = y-((x_sobel_kernel.length-1)/2);bl_y<=y+((x_sobel_kernel.length-1)/2);bl_y++){
			int kern_x=0;
			for (int bl_x = x-((x_sobel_kernel[0].length-1)/2);bl_x<=x+((x_sobel_kernel[0].length-1)/2);bl_x++){
				neighbor_x+=((blurred_img.getRGB(bl_x,bl_y)&0xff)*x_sobel_kernel[kern_y][kern_x]);
				kern_x++;
			}
			kern_y++;
		}
		int neighbor_y = 0;
		int kern_y1=0;
		//iterating pixels around picked pixel according to y kernel (finding vertical edges)
		for (int bl_y = y-((y_sobel_kernel.length-1)/2);bl_y<=y+((y_sobel_kernel.length-1)/2);bl_y++){
			int kern_x=0;
			for (int bl_x = x-((y_sobel_kernel[0].length-1)/2);bl_x<=x+((y_sobel_kernel[0].length-1)/2);bl_x++){
				neighbor_y+=((blurred_img.getRGB(bl_x,bl_y)&0xff)*y_sobel_kernel[kern_y1][kern_x]);
				kern_x++;
			}
			kern_y1++;
		}

		int sobel = (int) Math.sqrt(neighbor_x*neighbor_x+neighbor_y*neighbor_y);//calculating the edge out of x and y edges
		if(sobel<THRESHOLD)//SETTING THRESHOLD TO REDUCE NOISE
			sobel=0;
		sobel = (255<<24)|(sobel<<16)|(sobel<<8)|sobel;

		if(neighbor_y!=0&&neighbor_x!=0){//filling edge direction array (for Canny edge etector)
			//calculating the angle pf the edge and converting it to degrees
			angle[y][x]=(int)((Math.atan(neighbor_y/neighbor_x)*(180/3.14)));

			if((-22.5 <angle[y][x]&&angle[y][x]<22.5)||((-180<angle[y][x]&&angle[y][x]<-157.5)||(157.5<angle[y][x]&&angle[y][x]<180)))
				angle[y][x]=0;
			if((22.5 <angle[y][x]&&angle[y][x]<67.5)||(-157.5<angle[y][x]&&angle[y][x]<-112.5))
				angle[y][x]=45;
			if((67.5 <angle[y][x]&&angle[y][x]<112.5)||(-112.5<angle[y][x]&&angle[y][x]<-67.5))
				angle[y][x]=90;
			if((112.5<angle[y][x]&&angle[y][x]<157.5)||(-67.5<angle[y][x]&&angle[y][x]<-22.5))
				angle[y][x]=135;
		}

		img.setRGB(x,y,sobel);
	}
}

//********************************Canny Edge Detection********************************	
int gradient_max_x;
int gradient_max_y;
int alpha;
int beta;
for(int y = crop_range;y<img_y-crop_range;y++){
	for(int x = crop_range;x<img_x-crop_range;x++){
		if((img.getRGB(x,y)&0xff)!=0){
			switch(angle[y][x]){
			case 0:
			alpha = x;	
			gradient_max_x=x;
			x++;//setting pointer to the next pixel
			try{
				//while level(brightness) is greater then 0 and edge is vertical and not out of bound do
			while(((img.getRGB(x, y)&0xff)>0)&&(angle[y][x]==0)&&x<img_x-1){
				//if current pixel is greater then previously found max
				if((img.getRGB(x, y)&0xff)>gradient_max_x){
					img.setRGB(gradient_max_x, y, 0);//set previous max pixel to 0
					gradient_max_x = x;//set current pixel as new max
				}
				else{
					img.setRGB(x, y, 0);}//set current pixel to 0
				x++;//increment pointer
			}
		img.setRGB(gradient_max_x, y, (255<<24)|(255<<16)|(255<<8)|255);//set local maximum to 255
			}
			catch(Exception e){
			System.out.println("Canny warning!"+" "+e+" ("+x+";"+y+")");
			break;	
			}
			break;	
			case 45:
				beta = y;
				alpha= x;
				gradient_max_x=alpha;
				gradient_max_y=beta;
				alpha++;
				beta++;//setting pointer to the next pixel
				//while level(brightness) is greater then 0 and edge is vertical and not out of bound do
				while(((img.getRGB(alpha, beta)&0xff)>0)&&(angle[beta][alpha]==45)){
					//if current pixel is greater then previously found max
					if((img.getRGB(alpha, beta)&0xff)>(img.getRGB(gradient_max_x, gradient_max_y)&0xff)){
						img.setRGB(gradient_max_x, gradient_max_y, 0);//set previous max pixel to 0
						gradient_max_y = beta;//set current pixel as new max
						gradient_max_x = alpha;
					}
					else{
						img.setRGB(alpha, beta, 0);}//set current pixel to 0
					beta++;//increment pointer
					alpha++;
				}
			img.setRGB(gradient_max_x, gradient_max_y, (255<<24)|(255<<16)|(255<<8)|255);
			break;
			case 90:
				beta = y;
				gradient_max_y=beta;
				beta++;//setting pointer to the next pixel
				//while level(brightness) is greater then 0 and edge is vertical and not out of bound do
				while(((img.getRGB(x, beta)&0xff)>0)&&(angle[beta][x]==90)){
					//if current pixel is greater then previously found max
					if((img.getRGB(x, beta)&0xff)>gradient_max_y){
						img.setRGB(x, gradient_max_y, 0);//set previous max pixel to 0
						gradient_max_y = beta;//set current pixel as new max
					}
					else{
						img.setRGB(x, beta, 0);}//set current pixel to 0
					beta++;//increment pointer
				}
				img.setRGB(x, gradient_max_y, (255<<24)|(255<<16)|(255<<8)|255);	
			
			break;
			case 135:
				beta = y;
				alpha= x;
				gradient_max_x=alpha;
				gradient_max_y=beta;
				alpha--;
				beta++;//setting pointer to the next pixel
				//while level(brightness) is greater then 0 and edge is vertical and not out of bound do
				while(((img.getRGB(alpha, beta)&0xff)>0)&&(angle[beta][alpha]==135)){
					//if current pixel is greater then previously found max
					if((img.getRGB(alpha, beta)&0xff)>(img.getRGB(gradient_max_x, gradient_max_y)&0xff)){
						img.setRGB(gradient_max_x, gradient_max_y, 0);//set previous max pixel to 0
						gradient_max_y = beta;//set current pixel as new max
						gradient_max_x = alpha;
					}
					else{
						img.setRGB(alpha, beta, 0);}//set current pixel to 0
					beta++;//increment pointer
					alpha--;
				}
				img.setRGB(gradient_max_x, gradient_max_y, (255<<24)|(255<<16)|(255<<8)|255);
			}
		}
	}
}

//********************************Cropping_around_the_edge********************************		
for(int y = 0;y<img_y;y++){
	for(int x = 0;x<img_x;x++){
		if ((x<crop_range)||(x>=img_x-crop_range)||(y<crop_range)||(y>=img_y-crop_range)){	
			col=(255<<24)|(0<<16)|(0<<8)|0;	
			img.setRGB(x, y, col);			
		}
	}
}
System.out.println(System.currentTimeMillis()-start+" mills");// print running time in mills
String imgname = path.substring( 0, path.indexOf("."));
File output = new File("Source/"+imgname+"Out.png");//saving image as out.png
ImageIO.write(img, "PNG", output);
}
catch(Exception e){
	System.out.println("Error! - "+e); 
}	

}
}
