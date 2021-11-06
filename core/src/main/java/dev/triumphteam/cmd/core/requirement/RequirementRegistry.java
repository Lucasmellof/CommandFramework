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
package dev.triumphteam.cmd.core.requirement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry used for registering new requirements for all commands to use.
 *
 * @param <S> The sender type.
 */
public final class RequirementRegistry<S> {

    private final Map<RequirementKey, RequirementResolver<S>> requirements = new HashMap<>();

    public void register(
            @NotNull final RequirementKey key,
            @NotNull final RequirementResolver<S> resolver
    ) {
        requirements.put(key, resolver);
    }

    /**
     * Gets the {@link RequirementResolver} for the specific Key.
     *
     * @param key The specific key.
     * @return A saved {@link RequirementResolver}.
     */
    @Nullable
    public RequirementResolver<S> getRequirement(@NotNull final RequirementKey key) {
        return requirements.get(key);
    }

}