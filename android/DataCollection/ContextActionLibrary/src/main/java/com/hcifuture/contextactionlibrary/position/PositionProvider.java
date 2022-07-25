package com.hcifuture.contextactionlibrary.position;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hcifuture.contextactionlibrary.position.recorder.NormalRecorder;
import com.hcifuture.contextactionlibrary.position.scenario.Scenario;
import com.hcifuture.contextactionlibrary.position.scenario.ScenarioAnalyzer;
import com.hcifuture.contextactionlibrary.position.trigger.NormalTrigger;

import org.json.JSONException;

import java.util.concurrent.Executors;

public class PositionProvider {
    private static final String TAG = "PositionProvider";
    private final Context context;
    private ScenarioAnalyzer scenarioAnalyzer;
    private NormalRecorder normalRecorder;
    private NormalTrigger normalTrigger;

    private RecognizeCompleteListener recognizeCompleteListener;

    public PositionProvider(Context context) {
        this.context = context;
    }

    public void init() {
        Log.i(TAG, "init: ");

        scenarioAnalyzer = new ScenarioAnalyzer(context);
        scenarioAnalyzer.init();

        normalRecorder = new NormalRecorder(context);
        normalRecorder.init();
        scenarioAnalyzer.setRecorder(normalRecorder);

        normalTrigger = new NormalTrigger(context);
        normalTrigger.init();
        scenarioAnalyzer.setTrigger(normalTrigger);

        // 关于地点信息有些重要告警会以 Toast 形式出现 可以在此关掉
        NotificationUtility.setAllowToast(false);
    }

    public void start() {
        Log.i(TAG, "start: ");
        normalTrigger.start();
    }

    public void stop() {
        Log.i(TAG, "stop: ");
        normalTrigger.stop();
    }

    public void recognizePositionByCache() {
        scenarioAnalyzer.recognizePosition();
    }

    public Scenario getCurrentScenario() {
        return scenarioAnalyzer.getCurrentScenario();
    }

    public void recognizePositionByLatest() {
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));
        ListenableFuture<Scenario> process = service.submit(() -> {
            if (scenarioAnalyzer.isRunning()) {
                long timeout = 1000;
                while (timeout > 0) {
                    if (scenarioAnalyzer.isWIFIUpdated()) {
                        scenarioAnalyzer.recognizePosition();
                        return scenarioAnalyzer.getCurrentScenario();
                    } else {
                        Thread.sleep(100);
                        timeout -= 100;
                    }
                }
            }
            scenarioAnalyzer.recognizePosition();
            return scenarioAnalyzer.getCurrentScenario();
        });

        Futures.addCallback(
                process,
                new FutureCallback<Scenario>() {
                    @Override
                    public void onSuccess(Scenario scenario) {
                        if (recognizeCompleteListener != null) {
                            recognizeCompleteListener.onRecognizeComplete(scenario);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                    }
                },
                service);
    }

    public void setRecognizeCompleteListener(RecognizeCompleteListener recognizeCompleteListener) {
        this.recognizeCompleteListener = recognizeCompleteListener;
    }

    public void setExampleRecognizeCompleteListener() {
        this.recognizeCompleteListener = new RecognizeCompleteListener() {
            @Override
            public void onRecognizeComplete(Scenario scenario) {
                switch (scenario.recognizedPositionStatus) {
                    case Scenario.STATUS_UNFINISHED:
                        Log.i(TAG, "onRecognizeComplete: The ScenarioAnalyzer did not start for the very first time.");
                        break;
                    case Scenario.PROBLEM_MISSING_INFORMATION:
                        Log.i(TAG, "onRecognizeComplete: There is no position profile in storage.");
                        break;
                    case Scenario.RESULT_INVALID:
                        Log.i(TAG, "onRecognizeComplete: The sensor data is too old to recognize a position.");
                        break;
                    case Scenario.RESULT_NOT_FOUND:
                        Log.i(TAG, "onRecognizeComplete: This seems to be a new position.");
                        break;
                    case Scenario.RESULT_FOUND:
                        Log.i(TAG, "onRecognizeComplete: This is a recorded position.");
                        try {
                            double confidence = scenario.recognizedPositionConfidence;
                            String positionName = scenario.recognizedPositionProfile.getString("name");
                        } catch (JSONException e) {
                            Log.e(TAG, "onRecognizeComplete: ", e);
                            e.printStackTrace();
                        }
                        break;
                    default:
                        Log.i(TAG, "onRecognizeComplete: This should not appear.");
                }
            }
        };
    }

    public interface RecognizeCompleteListener {
        void onRecognizeComplete(Scenario scenario);
    }
}
