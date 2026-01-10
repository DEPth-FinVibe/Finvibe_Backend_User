package depth.finvibe.user.modules.user.application.port.out;

public interface TokenResolver {
    boolean isTokenValid(String token);
}
