package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import java.io.File;

import jd.plugins.Plugin;

import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeSolver;

public class BasicCaptchaChallenge extends ImageCaptchaChallenge<String> {

    public BasicCaptchaChallenge(final String method, final File file, final String defaultValue, final String explain, Plugin plugin, int flag) {
        super(file, method, explain, plugin);

    }

    @Override
    public AbstractResponse<String> parseAPIAnswer(String result, String resultFormat, ChallengeSolver<?> solver) {
        // String res = JSonStorage.restoreFromString("\"" + json + "\"", TypeRef.STRING);
        return new CaptchaResponse(this, solver, result, 100);

    }

}
