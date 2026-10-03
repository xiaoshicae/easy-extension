package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionNotFoundException;
import io.github.xiaoshicae.extension.core.exception.SessionParamException;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.ResolutionEntry;

import java.util.List;

public interface IScopedSessionManager {

    /**
     * Set scoped matched code with priority into session.
     *
     * @param scope    namespace of session area
     * @param code     code of matched business or matched business's used ability
     * @param priority priority of matched business or matched business's used ability
     * @throws SessionParamException if {@code code} is null or {@code priority} is null
     */
    void setScopedMatchedCode(String scope, String code, Integer priority) throws SessionException;

    /**
     * Get scoped matched codes from session.
     *
     * @param scope namespace of session area
     * @return codes of matched business and matched business's used abilities
     * @throws SessionNotFoundException matched codes is empty
     */
    List<String> getScopedMatchedCodes(String scope) throws SessionException;

    /**
     * Clear session with specific scope.
     *
     * @param scope namespace of session area
     */
    void removeScopedSession(String scope);

    /**
     * Clear all scoped session.
     */
    void removeAllSession();

    /**
     * Check if a scoped session exists and has data.
     *
     * @param scope namespace of session area
     * @return true if the scoped session exists and is not empty
     */
    boolean hasScopedSession(String scope);

    /**
     * Bind an already resolved chain to the scope, replacing whatever the scope held.
     * <p>
     * The default implementation replays the chain through {@link #setScopedMatchedCode(String, String, Integer)},
     * so existing implementations keep working. Override it to store the (immutable) chain as it is, which
     * saves building the session entry by entry on every request.
     * </p>
     *
     * @param scope namespace of session area
     * @param chain resolved chain
     * @throws SessionParamException if {@code scope} or {@code chain} is null
     * @since 4.0
     */
    default void bindScopedChain(String scope, ResolvedChain chain) throws SessionException {
        if (scope == null) {
            throw new SessionParamException("scope should not be null");
        }
        if (chain == null) {
            throw new SessionParamException("chain should not be null");
        }
        removeScopedSession(scope);
        for (ResolutionEntry entry : chain.entries()) {
            setScopedMatchedCode(scope, entry.code(), entry.priority());
        }
    }

    /**
     * The chain bound to the scope, if any.
     *
     * @param scope namespace of session area
     * @return the chain, or {@code null} when the scope holds none, or was not bound as a chain
     * @throws UnsupportedOperationException if this session manager does not keep chains
     * @since 4.0
     */
    default ResolvedChain getScopedChain(String scope) {
        throw new UnsupportedOperationException("getScopedChain() is not supported by this session manager");
    }
}
