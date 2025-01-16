package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionNotFoundException;
import io.github.xiaoshicae.extension.core.exception.SessionParamException;

import java.util.List;

public interface ISession {

    /**
     * Set matched code with priority into session.
     *
     * @param code     code of matched business or matched business's used ability
     * @param priority priority of matched business or matched business's used ability
     * @throws SessionParamException if {@code code} is null or {@code priority} is null
     */
    void setMatchedCode(String code, Integer priority) throws SessionException;

    /**
     * Get matched codes from session.
     *
     * @return codes of matched business and matched business's used abilities
     * @throws SessionNotFoundException matched codes is empty
     */
    List<String> getMatchedCodes() throws SessionException;

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
     * Clear all session.
     */
    void removeAllSession();

    /**
     * Clear session not include scoped session.
     */
    void removeSession();

    /**
     * Clear all scoped session.
     */
    void removeAllScopedSession();

    /**
     * Clear scoped session.
     *
     * @param scope namespace of session area
     */
    void removeScopedSession(String scope);
}
