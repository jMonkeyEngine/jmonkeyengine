package jme3test.blender.config;

import java.util.logging.Level;

import com.jme3.asset.ModelKey;

/**
 * This interface provides a method that allows to execute a method after the config has been properly set. It actually runs the test
 * itself.
 * @author Marcin Roguski (Kaelthas)
 */
public interface IConfigExecutable {
	/**
	 * This method runs the test with the given blender key.
	 * @param modelKey
	 *            the model key
	 * @param logLevel
	 *            the jme3 logger log level
	 */
	void execute(ModelKey modelKey, Level logLevel);
}
