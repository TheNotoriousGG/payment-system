import org.sonatype.nexus.security.realm.RealmManager
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.anonymous.AnonymousManager

def anonymousManager = container.lookup(AnonymousManager.class.name)
anonymousManager.setEnabled(false)
log.info("Anonymous access disabled automatically.")