package nl.vpro.api.rs.util;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Date: 13-3-12
 * Time: 17:54
 *
 * @author Ernst Bunders
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class TestBean {

//    @XmlAttribute
    private int anInt = 10;
//    @XmlAttribute
    private long aLong = 20L;
//    @XmlAttribute
    private boolean aBoolean = false;
    private float aFloat = 1.2F;
    private List<Friend> friends;
    @XmlElementWrapper(name = "friends")
    @XmlElement(name = "friend")

    private String aString = null;

    public TestBean() {
        friends = new ArrayList<Friend>();
        friends.add(new Friend("Bill", "Gates"));
        friends.add(new Friend("Bill", "Brison"));

    }

    public int getAnInt() {
        return anInt;
    }

    public void setAnInt(int anInt) {
        this.anInt = anInt;
    }

    public long getaLong() {
        return aLong;
    }

    public boolean isaBoolean() {
        return aBoolean;
    }

    public float getaFloat() {
        return aFloat;
    }

    public List<Friend> getFriends() {
        return friends;
    }

    public String getaString() {
        return aString;
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static final class Friend{
        private String firstName;
        private String lastName;

        public Friend() {
        }

        public Friend(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }
    }
}
