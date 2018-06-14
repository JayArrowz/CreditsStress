

import java.util.Random;

/**
 * @author Dil
 */
public final class RandomGenerator {
	private static final Random RANDOM = new Random();
	
	public static int random(int n) {
		return RandomGenerator.RANDOM.nextInt(n + 1);
	}
	
	public static int nextInt() {
		return RandomGenerator.RANDOM.nextInt();
	}
	
	public static int nextInt(int n) {
		return RandomGenerator.RANDOM.nextInt(n);
	}
	
	public static long nextLong() {
		return RandomGenerator.RANDOM.nextLong();
	}
	
	public static boolean nextBoolean() {
		return RandomGenerator.RANDOM.nextBoolean();
	}
	
	public static float nextFloat() {
		return RandomGenerator.RANDOM.nextFloat();
	}
	
	public static double nextDouble() {
		return RandomGenerator.RANDOM.nextDouble();
	}
	
}
