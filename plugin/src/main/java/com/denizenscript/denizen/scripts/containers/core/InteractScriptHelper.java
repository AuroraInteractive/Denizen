package com.denizenscript.denizen.scripts.containers.core;

import com.denizenscript.denizen.utilities.DenizenAPI;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizen.objects.NPCTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.scripts.commands.core.CooldownCommand;
import com.denizenscript.denizen.scripts.triggers.AbstractTrigger;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug.DebugElement;
import org.bukkit.ChatColor;

import java.util.List;

public class InteractScriptHelper {

    public static boolean debugGet = true;

    /**
     * Gets the InteractScript from an NPC and returns the appropriate ScriptContainer.
     * Returns null if no script found.
     *
     * @param npc     the NPC involved
     * @param player  the Player involved
     * @param trigger the class of the trigger being used
     * @return the highest priority InteractScriptContainer that meets requirements, if any.
     */
    public static InteractScriptContainer getInteractScript(NPCTag npc, PlayerTag player, Class<? extends AbstractTrigger> trigger) {
        if (npc == null || player == null || trigger == null) {
            return null;
        }
        AssignmentScriptContainer assignmentScript = npc.getAssignmentTrait().getAssignment();
        if (assignmentScript == null) {
            return null;
        }
        if (!assignmentScript.contains("interact scripts")) {
            return null;
        }
        List<String> assignedScripts = assignmentScript.getStringList("interact scripts");
        if (assignedScripts.isEmpty()) {
            return null;
        }
        if (Debug.shouldDebug(assignmentScript) && debugGet) {
            Debug.log(DebugElement.Header, "Getting interact script: n@" + npc.getName() + "/p@" + player.getName());
        }
        String script = assignedScripts.get(0);
        if (script.contains(" ") && Character.isDigit(script.charAt(0))) {
            Deprecations.interactScriptPriority.warn(assignmentScript);
            try {
                script = script.split(" ", 2)[1].replace("^", "");
            }
            catch (Exception e) {
                Debug.echoError("Invalid Interact assignment for '" + script + "'. Is the script name missing?");
                return null;
            }
        }
        InteractScriptContainer interactScript = ScriptRegistry.getScriptContainer(script);
        if (interactScript == null) {
            Debug.echoError("'" + script + "' is not a valid Interact Script. Is there a duplicate script by this name?");
            return null;
        }
        if (!CooldownCommand.checkCooldown(player, interactScript.getName())) {
            if (Debug.shouldDebug(interactScript) && debugGet) {
                Debug.log(ChatColor.GOLD + " ...but, isn't cooled down, yet! Skipping.");
                return null;
            }
        }
        if (Debug.shouldDebug(assignmentScript) && debugGet) {
            Debug.log(DebugElement.Spacer, null);
        }
        if (Debug.shouldDebug(assignmentScript) && debugGet) {
            Debug.log("Interact script is " + script + ". Current step for this script is: " + getCurrentStep(player, script));
        }
        if (Debug.shouldDebug(interactScript) && debugGet) {
            Debug.log(DebugElement.Footer, "");
        }
        return interactScript;
    }

    /**
     * Returns the current step for a Player and specified script. If no current step is found, the default
     * step is used, 'Default', unless another default (used by ending the step-name with a '*') is specified
     * in the script. For the sake of compatibility from v0.76, '1' can also be used.
     *
     * @param player     the Player to check
     * @param scriptName the name of the interact script container to check
     * @return the current, or default, step name
     */
    public static String getCurrentStep(PlayerTag player, String scriptName) {
        if (scriptName == null) {
            return null;
        }
        // Probe 'saves.yml' for the current step
        String step = DenizenAPI.getSaves().getString("Players." + player.getSaveName()
                + "." + "Scripts." + scriptName.toUpperCase() + "." + "Current Step");
        if (step != null) {
            return step.toUpperCase();
        }
        // No saved step found, so we'll just use the default
        return ScriptRegistry.getScriptContainerAs(scriptName, InteractScriptContainer.class).getDefaultStepName().toUpperCase();
    }
}

