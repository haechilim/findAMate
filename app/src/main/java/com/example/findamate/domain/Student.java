package com.example.findamate.domain;

import java.util.ArrayList;
import java.util.List;

public class Student {
    public final static int MAX_FAVORITE_SCORE = 3; // 최대 몇지망까지 입력받을지의 값

    private int id;
    private String name;
    private boolean male;
    private String phone;
    private int avatarId;
    private int score = 0;
    private double happiness = 0.0;
    private String statusMessage;
    private boolean hasPartner;
    private List<Student> favoritePartners = new ArrayList<>();
    private List<Student> partners = new ArrayList<>();
    private static int sequence = 0;

    public Student(String name, boolean male, String phone, int avatarId, String statusMessage) {
        this.id = sequence++;
        this.name = name;
        this.male = male;
        this.phone = phone;
        this.avatarId = avatarId;
        this.statusMessage = statusMessage;
    }

    public Student getFavoritePartner(int choice) {
        return choice >= 0 && choice < favoritePartners.size() ? favoritePartners.get(choice) : null;
    }

    public boolean isExpartner(Student partner) {
        for(int i = 0; i < partners.size(); i++) {
            if(partners.get(i).getId() == partner.getId()) return true;
        }

        return false;
    }

    public void clearHasPartner() {
        hasPartner = false;
    }

    public void addFavoritePartner(Student partner) {
        favoritePartners.add(partner);
    }

    public int getFavoritePartnerIndex(Student student) {
        for(int index = 0; index < favoritePartnersSize(); index++) {
            if(favoritePartners.get(index).getId() == student.getId()) return favoritePartnersSize() - index;
        }

        return 0;
    }

    public void addPartner(Student student) {
        partners.add(student);
    }

    public void addScore(int number) {
        score += number;

        calculateHappiness();
    }

    public int favoritePartnersSize() {
        return favoritePartners.size();
    }

    private void calculateHappiness() {
        int maxRound = Classroom.getMaxRound();

        if(score == -1) happiness = 0;
        else happiness = 100 - (MAX_FAVORITE_SCORE - (double)score / maxRound) * 20;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isHasPartner() {
        return hasPartner;
    }

    public void setHasPartner(boolean hasPartner) {
        this.hasPartner = hasPartner;
    }

    public boolean isMale() {
        return male;
    }

    public void setMale(boolean male) {
        this.male = male;
    }

    public List<Student> getFavoritePartners() {
        return favoritePartners;
    }

    public void setFavoritePartners(List<Student> favoritePartners) {
        this.favoritePartners = favoritePartners;
    }

    public List<Student> getPartners() {
        return partners;
    }

    public void setPartners(List<Student> partners) {
        this.partners = partners;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public double getHappiness() {
        return happiness;
    }

    public void setHappiness(double happiness) {
        this.happiness = happiness;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(int avatarId) {
        this.avatarId = avatarId;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", male=" + male +
                ", phone='" + phone + '\'' +
                ", avatarId=" + avatarId +
                ", score=" + score +
                ", happiness=" + happiness +
                ", statusMessage='" + statusMessage + '\'' +
                ", hasPartner=" + hasPartner +
                ", favoritePartners=" + favoritePartners +
                ", partners=" + partners +
                '}';
    }
}