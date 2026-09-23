package com.example.pollogithub.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.entity.UsuarioEntity;
import com.example.pollogithub.data.repository.PosRepository;

public class LoginViewModel extends AndroidViewModel {

    private final PosRepository repository;
    private final MutableLiveData<UsuarioEntity> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> needsTurnoApertura = new MutableLiveData<>(false);
    private final MutableLiveData<String> loginError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private UsuarioEntity lastUsuario;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        repository = PosRepository.getInstance(application);
    }

    public LiveData<UsuarioEntity> getLoginSuccess() { return loginSuccess; }
    public LiveData<Boolean> getNeedsTurnoApertura() { return needsTurnoApertura; }
    public LiveData<String> getLoginError() { return loginError; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public UsuarioEntity getLastUsuario() { return lastUsuario; }

    public void login(String userOrPin, String password) {
        isLoading.setValue(true);
        repository.login(userOrPin, password, new PosRepository.Callback<UsuarioEntity>() {
            @Override
            public void onSuccess(UsuarioEntity usuario) {
                lastUsuario = usuario;
                repository.getTurnoActivo(new PosRepository.Callback<TurnoEntity>() {
                    @Override
                    public void onSuccess(TurnoEntity turnoActivo) {
                        isLoading.setValue(false);
                        if (turnoActivo != null) {
                            repository.getSessionManager().saveSession(
                                    usuario.getId(),
                                    usuario.getNombreCompleto(),
                                    usuario.getRolId() == 1 ? "admin" : "cajero",
                                    turnoActivo.getId(),
                                    usuario.getSucursalId()
                            );
                            loginSuccess.setValue(usuario);
                        } else {
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
