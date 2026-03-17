package A3;

import java.security.SecureRandom;

/**
 * Class representing a die in the game.
 */
public class Die {
	private int value;
	private static final SecureRandom random = new SecureRandom();

	/**
	 * Constructor for Die class.
	 */
	public Die() {
		this.value = 1;
	}

	/**
	 * Rolls the die and returns the result.
	 * @return The result of the die roll.
	 */
	public int roll() {
		this.value = random.nextInt(6) + 1;
		return this.value;
	}

	public int getValue() {
		return value;
	}
}
