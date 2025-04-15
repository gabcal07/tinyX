import com.epita.events.UserActionEvent;
import com.epita.redis.CreatePostPublisherTest;
import com.epita.redis.CreatePostSubscriber;
import com.epita.redis.DeletePostPublisherTest;
import com.epita.redis.DeletePostSubscriber;
import com.epita.repository.entity.Post;
import com.epita.service.SearchService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ServiceSearchTests {

    @Inject
    SearchService searchService;

    @Inject
    CreatePostSubscriber createPostSubscriber;

    @Inject
    DeletePostSubscriber deletePostSubscriber;

    @Inject
    CreatePostPublisherTest createPostPublisherTest;

    @Inject
    DeletePostPublisherTest deletePostPublisherTest;

    String id1 = "7f3b3f0c-4d5d-4fbe-b44b-5736d8b51b3a";
    String text1 = "I am really excited to be here!";
    String id2 = "7f3b3f0c-4d5d-4fbe-b44b-5736d8b51b3b";
    String text2 = "I really excited to be here! #brandNew";
    String id3 = "7f3b3f0c-4d5d-4fbe-b44b-5736d8b51b3c";
    String text3 = "I am really #excited #to #be here! #brandNew";
    // exec a function after each test
    @BeforeEach
    public void deleteAllPostsBefore(){
        UserActionEvent u1 = new UserActionEvent();
        u1.setActionType(UserActionEvent.ActionType.POST_DELETED);
        u1.setPostId(id1);
        u1.setPostContent(text1);
        u1.setTimestamp(null);
        UserActionEvent u2 = new UserActionEvent();
        u2.setActionType(UserActionEvent.ActionType.POST_DELETED);
        u2.setPostId(id2);
        u2.setPostContent(text2);
        u2.setTimestamp(null);
        UserActionEvent u3 = new UserActionEvent();
        u3.setActionType(UserActionEvent.ActionType.POST_DELETED);
        u3.setPostId(id3);
        u3.setPostContent(text3);
        u3.setTimestamp(null);
        deletePostPublisherTest.publish(u1);
        deletePostPublisherTest.publish(u2);
        deletePostPublisherTest.publish(u3);
    }

    @Test
    public void testPostClassConstructorNoHashtags() {
        Post p = new Post(UUID.fromString(id1), text1);
        assertEquals(UUID.fromString(id1), p.getPostId());
        assertEquals(text1, p.getText());
        assertEquals(text1, p.getFullText());
        assertEquals(0, p.getHashtags().size());
    }

    @Test
    public void testPostClassConstructor1Hashtags() {
        String textWithoutHashtags = "I really excited to be here!";
        Post p = new Post(UUID.fromString(id2), text2);
        assertEquals(UUID.fromString(id2), p.getPostId());
        assertEquals(textWithoutHashtags, p.getText());
        assertEquals(text2, p.getFullText());
        assertEquals(1, p.getHashtags().size());
        List<String> hashtags = List.of("#brandNew");
        assertEquals(hashtags, p.getHashtags());
    }

    @Test
    public void testPostClassConstructornHashtags() {
        String textWithoutHashtags = "I am really here!";
        Post p = new Post(UUID.fromString(id3), text3);
        assertEquals(UUID.fromString(id3), p.getPostId());
        assertEquals(textWithoutHashtags, p.getText());
        assertEquals(text3, p.getFullText());
        List<String> hashtags = List.of("#excited", "#to", "#be", "#brandNew");
        assertEquals(hashtags, p.getHashtags());
    }

    @Test
    public void testWithRedis(){
        String id = "7f3b3f0c-4d5d-4fbe-b44b-5736d8b51b3a";
        String text = "I am really excited to be here!";
        UserActionEvent u = new UserActionEvent();
        u.setTimestamp(null);
        u.setPostId(id);
        u.setPostContent(text);
        u.setActionType(UserActionEvent.ActionType.POST_CREATED);
        createPostPublisherTest.publish(u);
        List<Post> posts = searchService.getAllPosts();
        assertEquals(1, posts.size());
        assertEquals(UUID.fromString(id), posts.get(0).getPostId());
        assertEquals(0, posts.get(0).getHashtags().size());
        assertEquals(text, posts.get(0).getText());
    }

    // for this test to run you need to start elastisearch with the docker file
    @Test
    public void testPostSimpleMessage(){
        UserActionEvent u = new UserActionEvent();
        u.setTimestamp(null);
        u.setActionType(UserActionEvent.ActionType.POST_CREATED);
        u.setPostId(id1);
        u.setPostContent(text1);
        createPostPublisherTest.publish(u);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<Post> posts = searchService.getAllPosts();
        assertEquals(1, posts.size());
        assertEquals(UUID.fromString(id1), posts.get(0).getPostId());
        assertEquals(0, posts.get(0).getHashtags().size());
        assertEquals(text1, posts.get(0).getText());
    }
    @Test
    public void testPostOneHashtag(){
        UserActionEvent u = new UserActionEvent();
        u.setTimestamp(null);
        u.setPostId(id2);
        u.setPostContent(text2);
        u.setActionType(UserActionEvent.ActionType.POST_CREATED);
        createPostPublisherTest.publish(u);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<Post> posts = searchService.getAllPosts();
        assertEquals(1, posts.size());
        assertEquals(UUID.fromString(id2), posts.get(0).getPostId());
        assertEquals(1, posts.get(0).getHashtags().size());
        assertEquals("I really excited to be here!", posts.get(0).getText());
    }
    @Test
    public void testPostNHashtag(){
        UserActionEvent u = new UserActionEvent();
        u.setTimestamp(null);
        u.setPostId(id3);
        u.setPostContent(text3);
        u.setActionType(UserActionEvent.ActionType.POST_CREATED);
        searchService.createPost(u);
    }
    @Test
    public void testSearchText(){
        UserActionEvent u1 = new UserActionEvent();
        u1.setActionType(UserActionEvent.ActionType.POST_CREATED);
        u1.setPostId(id1);
        u1.setPostContent(text1);
        u1.setTimestamp(null);
        UserActionEvent u2 = new UserActionEvent();
        u2.setActionType(UserActionEvent.ActionType.POST_CREATED);
        u2.setPostId(id2);
        u2.setPostContent(text2);
        u2.setTimestamp(null);
        UserActionEvent u3 = new UserActionEvent();
        u3.setActionType(UserActionEvent.ActionType.POST_CREATED);
        u3.setPostId(id3);
        u3.setPostContent(text3);
        u3.setTimestamp(null);
        searchService.createPost(u1);
        searchService.createPost(u2);
        searchService.createPost(u3);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String text = "I";
        List<Post> posts = searchService.searchPost(text);
        List<UUID> expectedIds = List.of(
                UUID.fromString(id1),
                UUID.fromString(id2),
                UUID.fromString(id3)
        );
        System.out.println(posts);
        assertEquals(3, posts.size());
        for (Post p : posts){
            assertTrue(expectedIds.contains(p.getPostId()));
        }
    }

}
