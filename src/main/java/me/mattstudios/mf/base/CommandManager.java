/*
 * MIT License
 *
 * Copyright (c) 2019 Matt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.mattstudios.mf.base;

import me.mattstudios.mf.annotations.Alias;
import me.mattstudios.mf.annotations.Command;
import me.mattstudios.mf.exceptions.NoCommandException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class CommandManager implements Listener {

    private JavaPlugin plugin;

    // List of commands;
    private Map<String, CommandHandler> commands;

    private ParameterHandler parameterHandler;
    private CompletionHandler completionHandler;
    private MessageHandler messageHandler;

    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        commands = new HashMap<>();
        completionHandler = new CompletionHandler();
        messageHandler = new MessageHandler();
        parameterHandler = new ParameterHandler();
    }

    /**
     * Gets the parameter types class to register new ones and to check too.
     *
     * @return The parameter types class.
     */
    public ParameterHandler getParameterHandler() {
        return parameterHandler;
    }

    /**
     * Gets the completion handler class, which handles all the command completions in the plugin.
     *
     * @return The completion handler.
     */
    public CompletionHandler getCompletionHandler() {
        return completionHandler;
    }

    /**
     * Gets the message handler, which handles all the messages autogenerated by the framework.
     *
     * @return The message handler.
     */
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    /**
     * Registers a command.
     *
     * @param command The command class to register.
     */
    public void register(CommandBase command) {
        Class commandClass = command.getClass();

        // Checks for the command annotation.
        if (!commandClass.isAnnotationPresent(Command.class))
            throw new NoCommandException("Class " + command.getClass().getName() + " needs to have @Command!");

        // Gets the command annotation value.
        String commandName = ((Command) commandClass.getAnnotation(Command.class)).value();
        String[] aliases = new String[0];

        //Checks if the class has some alias and adds them.
        if (commandClass.isAnnotationPresent(Alias.class))
            aliases = ((Alias) commandClass.getAnnotation(Alias.class)).value();

        // Used to get the command map to register the commands.
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);

            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            CommandHandler commandHandler;
            if (commands.containsKey(commandName)) {
                commands.get(commandName).addSubCommands(command);
                return;
            }

            commandHandler = new CommandHandler(parameterHandler, completionHandler, messageHandler, command, commandName, Arrays.asList(aliases));
            commandMap.register(plugin.getName(), commandHandler);

            // Puts the handler in the list to unregister later.
            commands.put(commandName, commandHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Unregisters all the commands on the disable of the plugin.
     *
     * @param event PluginDisableEvent.
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (!(plugin.getName().equalsIgnoreCase(event.getPlugin().getName()))) {
            return;
        }

        unregisterAll();
    }

    /**
     * Unregisters all commands.
     */
    private void unregisterAll() {
        commands.clear();
    }
}
