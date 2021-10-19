/**
 * MIT License
 *
 * Copyright (c) 2019-2021 Matt
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
package dev.triumphteam.cmds.core.message;

import dev.triumphteam.cmds.core.key.RegistryKey;
import dev.triumphteam.cmds.core.message.context.MessageContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Registry key, for more organized way of registering and getting things from the registries.
 */
public abstract class ContextualKey<C extends MessageContext> extends RegistryKey {

    // Holds all registered keys, default and custom ones
    private static final Set<MessageKey<? extends MessageContext>> REGISTERED_KEYS = new HashSet<>();

    private final Class<C> type;

    protected ContextualKey(@NotNull final String key, @NotNull final Class<C> type) {
        super(key);
        this.type = type;
        REGISTERED_KEYS.add((MessageKey<? extends MessageContext>) this);
    }

    /**
     * Getter for the Context type, in case it's needed.
     *
     * @return The Context type.
     */
    public Class<C> getType() {
        return type;
    }

    /**
     * Gets an immutable {@link Set} with all the registered keys.
     *
     * @return The keys {@link Set}.
     */
    @NotNull
    public static Set<MessageKey<? extends MessageContext>> getRegisteredKeys() {
        return Collections.unmodifiableSet(REGISTERED_KEYS);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ContextualKey<?> that = (ContextualKey<?>) o;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type);
    }

    @Override
    public String toString() {
        return "ContextualKey{" +
                "type=" + type +
                ", super=" + super.toString() + "}";
    }
}
