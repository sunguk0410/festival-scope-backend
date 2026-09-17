package likelion.festivalscope.auth.jwt;
import jakarta.servlet.FilterChain; import jakarta.servlet.ServletException; import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException; import java.util.Collections;
public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final JwtTokenProvider provider; public JwtAuthenticationFilter(JwtTokenProvider provider){this.provider=provider;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  String header=request.getHeader("Authorization"); if(header!=null&&header.startsWith("Bearer ")){String token=header.substring(7); if(provider.isValid(token)){Long userId=provider.getUserId(token); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId,null,Collections.emptyList()));}}
  chain.doFilter(request,response);
 }
}
