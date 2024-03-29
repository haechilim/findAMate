package com.example.findamate.activity;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.findamate.R;
import com.example.findamate.adapter.LogAdapter;
import com.example.findamate.domain.Classroom;
import com.example.findamate.domain.Couple;
import com.example.findamate.domain.History;
import com.example.findamate.domain.Poll;
import com.example.findamate.domain.Student;
import com.example.findamate.helper.Logger;
import com.example.findamate.helper.Util;
import com.example.findamate.manager.ApiManager;
import com.example.findamate.manager.PermissionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

public class PollActivity extends AppCompatActivity {
    public final static int TYPE_RESULT = 1;
    public final static int TYPE_SIMULATION = 2;

    private int type;
    private boolean isSimulation;
    private int mode;
    private boolean duplicated;
    private int agree;
    private int disagree;
    private int submits;
    private Timer timer;
    private List<Student> students;
    private List<History> histories;
    private TextView agreeView;
    private TextView disagreeView;
    private TextView pollRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll);

        Intent intent = getIntent();
        type = intent.getIntExtra("type", TYPE_SIMULATION);
        isSimulation = intent.getBooleanExtra("isSimulation", true);
        mode = intent.getIntExtra("mode", 1);
        duplicated = intent.getBooleanExtra("duplicated", false);

        if (type == LogActivity.TYPE_SIMULATION) {
            students = Classroom.getClonedStudents();
            histories = Classroom.getClonedHistories();
        }
        else {
            students = isSimulation ? Classroom.clonedStudents() : Classroom.students;
            histories = isSimulation ? Classroom.clonedHistories() : Classroom.histories;
        }

        agreeView = findViewById(R.id.agree);
        disagreeView = findViewById(R.id.disagree);
        pollRate = findViewById(R.id.pollRate);

        List<History> histories = new ArrayList<>();
        histories.add(new History(Classroom.couples));
        LogAdapter logAdapter = new LogAdapter(PollActivity.this, histories, true);
        ((ListView)findViewById(R.id.list)).setAdapter(logAdapter);

        runPoll();
        bindEvents();
    }

    private void runPoll() {
        updateSubmits(0);

        if(isSimulation) monitorPollFake();
        else {
            sendSms(true);

            poll(false, () -> {
                poll(true, () -> {
                    monitorPoll();
                });
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) sendSms(false);
        else Util.toast(PollActivity.this, "권한 거부로 인해 설문조사 기능이 제한됩니다.", true);
    }

    // 권한 확인 후 메시지 전송
    private void sendSms(boolean checkPermission) {
        if(checkPermission && !PermissionManager.isSmsGranted(this)) PermissionManager.requestSmsPermission(this);
        else {
            for(int i = 0; i < Classroom.couples.size(); i++) {
                Couple couple = Classroom.couples.get(i);
                Student student1 = couple.getStudent1();
                Student student2 = couple.getStudent2();

                sendPollSms(student1, student2);
                sendPollSms(student2, student1);
            }

            Util.toast(this, "학생들에게 문자메시지를 발송하였습니다.", false);
        }
    }

    private void sendPollSms(Student receiver, Student mate) {
        if(receiver == null) return;

        String url = String.format("%s?sid=%d&id=%d&mid=%d",
                ApiManager.HOST, ApiManager.getMemberId(),
                receiver != null ? receiver.getId() : -1,
                mate != null ? mate.getId() : -1);
        String message = "[내짝궁 설문] \n" + url;

        Util.sendSms(this, receiver.getPhone(), message);
    }

    @Override
    public void onBackPressed() {}

    private void bindEvents() {
        findViewById(R.id.again).setOnClickListener((v) -> {
            timer.cancel();
            startMatchingActivity();
        });

        findViewById(R.id.ok).setOnClickListener((v) -> {
            if(isSimulation) {
                addSimulationHistory();
                startLogActivity();
                finish();
                return;
            }

            poll(false, () -> {
                ApiManager.addRound(agree, disagree, (history) -> {
                    updateDb(history.getId());
                    timer.cancel();
                    startLogActivity();
                    finish();
                });
            });
        });
    }

    private void startLogActivity() {
        Intent intent = new Intent(PollActivity.this, LogActivity.class);
        intent.putExtra("type", type);
        startActivity(intent);
        finish();
    }

    private void startMatchingActivity() {
        Intent intent = new Intent(PollActivity.this, MatchingActivity.class);
        intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("type", type);
        intent.putExtra("mode", mode);
        intent.putExtra("isSimulation", isSimulation);
        intent.putExtra("duplicated", duplicated);
        startActivity(intent);
        finish();
    }

    // 설문 시작 or 종료
    private void poll(boolean begin, PollCallback callback) {
        ApiManager.poll(begin, (success) -> {
            callback.complete();
        });
    }

    // 타이머로 주기 적으로 데이터 요청 후 화면 갱신
     private void monitorPoll() {
         timer = new Timer();
         TimerTask timerTask = new TimerTask() {
             @Override
             public void run() {
                 runOnUiThread(() -> {
                     pollStatus();
                 });
             }
         };

         timer.schedule(timerTask, 0, 1000);
     }

    private void monitorPollFake() {
        List<Poll> polls = new ArrayList<>();
        Random random = new Random();

        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    polls.add(new Poll(random.nextBoolean()));
                    updatePollResult(polls);

                    if(submits >= students.size()) timer.cancel();
                });
            }
        };

        timer.schedule(timerTask, 3000, 3000);
    }

    // 학생들의 설문결과를 가져옴
    private void pollStatus() {
        ApiManager.pollStatus(new ApiManager.PollListCallback() {
            @Override
            public void success(List<Poll> polls) {
                updatePollResult(polls);
            }
        });
    }

    //좋아요, 다시해요, 응답율 갱신
    private void updatePollResult(List<Poll> polls) {
        Logger.debug(polls.toString());

        if(polls.isEmpty()) return;

        int count = 0;
        int submits = 0;

        for(int i = 0; i < polls.size(); i++) {
            if(polls.get(i).isAgree()) count++;
            submits++;
        }

        if(submits != 0) {
            int agree = Math.round((float) count / submits * 100);
            int disagree = 100 - agree;

            if(submits != this.submits) {
                this.agree = agree;
                this.disagree = disagree;
                this.submits = submits;

                startCountingAnimation(agreeView, "%d%%", agree);
                startCountingAnimation(disagreeView, "%d%%", disagree);
                updateSubmits(submits);
            }
        }
    }

    private void updateSubmits(int submits) {
        pollRate.setText(String.format("%d/%d명", submits, students.size()));
    }

    private void startCountingAnimation(TextView view, String format, int value) {
        ValueAnimator animator = new ValueAnimator();
        animator.setObjectValues(0, value);
        animator.addUpdateListener((animation) -> view.setText(String.format(format, animation.getAnimatedValue())));
        animator.setDuration(500);
        animator.start();
    }

    // 아이디를 객체와 연결
    private void refineHistories(List<History> histories) {
        for(int i = 0; i < histories.size(); i++) {
            History history = histories.get(i);
            List<Couple> couples = history.getCouples();

            for(int j = 0; j < couples.size(); j++) {
                Couple couple = couples.get(j);
                couple.setStudent1(Classroom.findStudentById(couple.getStudentId1(), false));
                couple.setStudent2(Classroom.findStudentById(couple.getStudentId2(), false));
            }
        }
    }

    private void addSimulationHistory() {
        histories.add(new History(Classroom.clonedCouples(), agree, disagree));
    }

    private void updateDb(int roundId) {
        for(int i = 0; i < Classroom.couples.size(); i++) {
            Couple couple = Classroom.couples.get(i);
            ApiManager.addMate(couple.getStudent1(), couple.getStudent2(), roundId);
        }

        for(int i = 0; i < students.size(); i++) {
            ApiManager.modifyStudent(students.get(i), null);
        }
    }

    private interface PollCallback {
        void complete();
    }
}