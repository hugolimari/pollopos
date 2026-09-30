package com.example.pollogithub.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.entity.UsuarioEntity;
import com.example.pollogithub.data.repository.PosRepository;

/**
 * ViewModel para gestionar el login y validación de estado de turno.
 */
public class LoginViewModel extends AndroidViewModel {

    /**
     * Referencia a la capa de abstracción de datos (Repositorio).
     */
    private final PosRepository repository;

    /**
     * Contenedor observable de autenticación exitosa.
     */
    private final MutableLiveData<UsuarioEntity> loginSuccess = new MutableLiveData<>();

    /**
     * Contenedor observable que señaliza la exigencia de apertura de caja antes de operar el POS.
     */
    private final MutableLiveData<Boolean> needsTurnoApertura = new MutableLiveData<>(false);

    /**
     * Contenedor observable para mensajes de error de credenciales o comunicación.
     */
    private final MutableLiveData<String> loginError = new MutableLiveData<>();

    /**
     * Indicador de progreso para renderizado de Spinners o barras de carga en la UI.
     */
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    /**
     * Registro en memoria del último usuario autenticado en la sesión actual.
     */
    private UsuarioEntity lastUsuario;

    /**
     * Constructor con inyección de contexto de aplicación para inicializar el repositorio.
     * 
     * @param application Contexto global para evitar fugas de memoria (Memory Leaks).
     */
    public LoginViewModel(@NonNull Application application) {
        super(application);
        repository = PosRepository.getInstance(application);
    }

    // ==========================================
    // GETTERS DE FLUJOS REACTIVOS (OBSERVABLES)
    // Exposición inmutable hacia la Activity
    // ==========================================

    public LiveData<UsuarioEntity> getLoginSuccess() { return loginSuccess; }
    public LiveData<Boolean> getNeedsTurnoApertura() { return needsTurnoApertura; }
    public LiveData<String> getLoginError() { return loginError; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public UsuarioEntity getLastUsuario() { return lastUsuario; }

    /**
     * Ejecuta el flujo asíncrono de autenticación de usuario.
     * Al validar credenciales, realiza una consulta encadenada para verificar el estado de turno de caja:
     * - Si hay turno activo: persiste la sesión completa y emite 'loginSuccess'.
     * - Si no hay turno activo: persiste la sesión con turno 0 y emite 'needsTurnoApertura'.
     * 
     * @param userOrPin Nombre de usuario formal o código PIN rápido.
     * @param password  Contraseña de acceso (o cadena vacía si es ingreso por PIN).
     */
    public void login(String userOrPin, String password) {
        isLoading.setValue(true);
        repository.login(userOrPin, password, new PosRepository.Callback<UsuarioEntity>() {
            @Override
            public void onSuccess(UsuarioEntity usuario) {
                lastUsuario = usuario;
                // Verificación de invariante de negocio: existencia de turno operativo
                repository.getTurnoActivo(new PosRepository.Callback<TurnoEntity>() {
                    @Override
                    public void onSuccess(TurnoEntity turnoActivo) {
                        isLoading.setValue(false);
                        if (turnoActivo != null) {
                            // Turno previamente abierto: sesión completamente operativa
                            repository.getSessionManager().saveSession(
                                    usuario.getId(),
                                    usuario.getNombreCompleto(),
                                    usuario.getRolId() == 1 ? "admin" : "cajero",
                                    turnoActivo.getId(),
                                    usuario.getSucursalId()
                            );
                            loginSuccess.setValue(usuario);
                        } else {
                            // Sin turno activo: requiere registro de fondo de caja
                            repository.getSessionManager().saveSession(
                                    usuario.getId(),
                                    usuario.getNombreCompleto(),
                                    usuario.getRolId() == 1 ? "admin" : "cajero",
                                    0,
                                    usuario.getSucursalId()
                            );
                            needsTurnoApertura.setValue(true);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        isLoading.setValue(false);
                        needsTurnoApertura.setValue(true);
                    }
                });
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                loginError.setValue(error);
            }
        });
    }
}
