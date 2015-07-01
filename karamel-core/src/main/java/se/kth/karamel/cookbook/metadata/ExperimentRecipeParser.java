package se.kth.karamel.cookbook.metadata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.kth.karamel.backend.ExperimentContext.ScriptType;
import se.kth.karamel.common.exception.RecipeParseException;

public class ExperimentRecipeParser {

  public static Pattern EXPERIMENT_SCRIPT = Pattern.compile("script \'run_experiment\'");
  public static Pattern EXPERIMENT_DELIMITER = Pattern.compile("EOM");

  /**
   *
   * @param content
   * @return an experiment recipe
   * @throws se.kth.karamel.common.exception.RecipeParseException
   */
  public static ExperimentRecipe parse(String content) throws RecipeParseException {
//    String[] lines = content.split("\\r?\\n");

    Matcher mp = EXPERIMENT_SCRIPT.matcher(content);
    boolean foundPreScript = mp.find();
    if (!foundPreScript) {
      throw new RecipeParseException(
          "Could not find in the recipe any chef code before a script resource like \"script 'run_experiment' do\" ");
    }
    String preScript = content.substring(0, mp.start());
    String postScript = content.substring(mp.start());
    
    Matcher ms = EXPERIMENT_DELIMITER.matcher(postScript);
    boolean foundStart = ms.find();
    if (!foundStart) {
      throw new RecipeParseException(
          "Could not find in the recipe a script resource like \"script 'run_experiment' do\" ");
    }
    int startPos = ms.end();

    boolean foundEnd = ms.find();
    if (!foundEnd) {
      throw new RecipeParseException(
          "Could not find in the recipe a script resource like \"script 'run_experiment' do\" ");
    }
    int endPos = ms.start();
    
    String script = postScript.substring(startPos, endPos);

    return new ExperimentRecipe("experiment", ScriptType.bash, script, preScript);
  }

}
