package com.example.pollogithub;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.pollogithub.ui.viewmodel.LoginViewModel;

/**
 * Controlador de Vista: MainActivity (Pantalla de Autenticación / Login)
 * 
 * Capa de Presentación / Controlador de Entrada (View en MVVM)
 * Hereda de: AppCompatActivity
 * 
 * Punto de entrada principal a la aplicación móvil. Implementa el formulario de
 * captura de credenciales para cajeros y personal administrativo, gestionando
 * la alternancia de visibilidad de contraseña, validaciones sintácticas en cliente
 * y la observación reactiva de los estados emitidos por LoginViewModel.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Patrón MVVM: La actividad actúa como vista pasiva; delega la validación de negocio y acceso a datos a 'LoginViewModel'.
 * - Ciclo de Vida y Persistencia de Estado: Suscripción a LiveData con ciclo de vida vinculado (LifecycleOwner = this).
 * - Enrutamiento Condicional según Reglas de Negocio:
 *     * Si el usuario se autentica y ya tiene un turno abierto -> Navega a HomeActivity.
 *     * Si el usuario se autentica pero no tiene turno activo -> Navega a AperturaCajaActivity para forzar el arqueo inicial.
 * - Soporte Edge-To-Edge: Adaptación de la interfaz a las barras del sistema (System Bars / WindowInsets) para compatibilidad con Android 14/15+.
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Bandera para el control de visualización en texto plano u oculto de la contraseña.
     */
    private boolean isPasswordVisible = false;

    /**
     * Instancia del ViewModel obtenida mediante ViewModelProvider para retención ante rotación de pantalla.
     */
    private LoginViewModel loginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Habilita diseño moderno de borde a borde (Edge-to-Edge)
        EdgeToEdge.enable(this);

        // Verificación de sesión activa (duración de 4 horas continuas)
        com.example.pollogithub.data.SessionManager sessionManager = new com.example.pollogithub.data.SessionManager(this);
        if (sessionManager.isSessionActive()) {
            int turnoId = sessionManager.getTurnoId();
            if (turnoId > 0) {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                intent.putExtra("USER_NAME", sessionManager.getUserName());
                startActivity(intent);
                finish();
                return;
            } else {
                Intent intent = new Intent(MainActivity.this, AperturaCajaActivity.class);
                intent.putExtra("USER_NAME", sessionManager.getUserName());
                intent.putExtra("USER_ID", sessionManager.getUserId());
                intent.putExtra("SUCURSAL_ID", sessionManager.getSucursalId());
                startActivity(intent);
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_main);

        // Compensación de márgenes para prevenir solapamiento con barras de estado y navegación
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Inicialización del ViewModel mediante el Provider de AndroidX
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // 2. Enlace de referencias a componentes gráficos del layout
        EditText etUser = findViewById(R.id.etUser);
        EditText etPassword = findViewById(R.id.etPassword);
        ImageButton btnToggleEye = findViewById(R.id.btnToggleEye);

        // 3. Mecanismo de alternancia de visualización de contraseña mediante máscaras de bits InputType
        btnToggleEye.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnToggleEye.setImageResource(R.drawable.ic_eye);
                isPasswordVisible = false;
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnToggleEye.setImageResource(R.drawable.ic_eye_off);
                isPasswordVisible = true;
            }
            // Mantiene el cursor al final de la cadena de texto ingresada
            etPassword.setSelection(etPassword.getText().length());
        });

        // 4. Navegación hacia recuperación de credenciales
        findViewById(R.id.tvForgotPin).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecuperarPasswordActivity.class);
            startActivity(intent);
        });

        // 5. Suscripción a observadores reactivos (LiveData) del ViewModel

        // Observador: Inicio de sesión exitoso con turno previamente abierto
        loginViewModel.getLoginSuccess().observe(this, usuario -> {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            intent.putExtra("USER_NAME", usuario.getNombreCompleto());
            startActivity(intent);
            finish(); // Finaliza la actividad de login para evitar volver atrás en el stack de navegación
        });

        // Observador: Requiere apertura formal de caja chica
        loginViewModel.getNeedsTurnoApertura().observe(this, needs -> {
            if (Boolean.TRUE.equals(needs)) {
                Intent intent = new Intent(MainActivity.this, AperturaCajaActivity.class);
                if (loginViewModel.getLastUsuario() != null) {
                    intent.putExtra("USER_NAME", loginViewModel.getLastUsuario().getNombreCompleto());
                    intent.putExtra("USER_ID", loginViewModel.getLastUsuario().getId());
                    intent.putExtra("SUCURSAL_ID", loginViewModel.getLastUsuario().getSucursalId());
                }
                startActivity(intent);
                finish();
            }
        });

        // Observador: Errores de validación o credenciales inválidas
        loginViewModel.getLoginError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // 6. Validación sintáctica de inputs y despacho de acción de autenticación
        findViewById(R.id.btnStartShift).setOnClickListener(v -> {
            String user = etUser.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (user.isEmpty()) {
                etUser.setError("Usuario requerido");
                return;
            }
            if (pass.isEmpty()) {
                etPassword.setError("Contraseña requerida");
                return;
            }

            // Delega la ejecución del login asíncrono al ViewModel
            loginViewModel.login(user, pass);
        });
    }
}