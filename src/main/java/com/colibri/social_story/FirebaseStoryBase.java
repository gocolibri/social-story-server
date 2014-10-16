package com.colibri.social_story;

import com.colibri.social_story.entities.*;
import com.colibri.social_story.utils.Pair;
import com.firebase.client.*;
import com.google.gson.Gson;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class FirebaseStoryBase implements StoryBase {

    private final Firebase fb;

    public FirebaseStoryBase(Firebase fb) {
        this.fb = fb;
    }

    private void syncClear(String path) throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        fb.child(path).removeValue(new ReleaseLatchCompletionListener(done));
        done.await();
    }

    private Pair<String, Object> syncGetLeafFromRoot(String path) throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        final Pair<String, Object> value = new Pair<>();
        fb.getRoot().child(path).addValueEventListener( new FirebaseValueEventListenerAdapter() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                value.fst = dataSnapshot.getName();
                value.snd = dataSnapshot.getValue();
                done.countDown();
            }
        });
        done.await();
        return value;
    }

    @Override
    public void onUserAdded(final StoryBaseCallback storyBaseCallback) {
        fb.child("users").addChildEventListener(new FirebaseChildEventListenerAdapter() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                storyBaseCallback.handle(dataSnapshot.getName());
            }});
    }

    @Override
    public void onWordAdded(final StoryBaseCallback storyBaseCallback) {
        fb.child("suggestions").addChildEventListener(new FirebaseChildEventListenerAdapter() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                storyBaseCallback.handle(
                        new Pair<>(dataSnapshot.getName(),
                                   dataSnapshot.getValue()));
            }
        });
    }

    @Override
    public void onVotesAdded(final StoryBaseCallback storyBaseCallback) {
        fb.child("votes").addChildEventListener(new FirebaseChildEventListenerAdapter() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                storyBaseCallback.handle(
                        new Pair<>(dataSnapshot.getName(),
                                   dataSnapshot.getValue()));
            }
        });
    }

    @Override
    public long getServerOffsetMillis() throws InterruptedException {
        Pair<String, Object> offset = syncGetLeafFromRoot(".info/serverTimeOffset");
        return new Date().getTime() + (Long)offset.snd;
    }

    public void writeStoryAttributes(Story story) {
        try {
            final CountDownLatch done = new CountDownLatch(1);
            fb.setValue(story, new Firebase.CompletionListener() {
                @Override
                public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                    done.countDown();
                }
            });
            done.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeStory() {
        try {
            syncClear("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static class ReleaseLatchCompletionListener implements Firebase.CompletionListener {
        private final CountDownLatch done;

        public ReleaseLatchCompletionListener(CountDownLatch done) {
            this.done = done;
        }

        @Override
        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
            done.countDown();
        }
    }


}