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
     * Clear session.
     */
    void remove();
}
