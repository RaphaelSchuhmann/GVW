<script>
    import { push } from "svelte-spa-router";
    import Logo from "../../assets/logo.svg";
    import Input from "../../components/Input.svelte";
    import Button from "../../components/Button.svelte";
    import ToastStack from "../../components/ToastStack.svelte";
    import { clearValue, getValue, setValue } from "../../services/store";
    import { auth } from "../../stores/auth.svelte.js";
    import { user } from "../../stores/user.svelte";
    import { addToast } from "../../stores/toasts.svelte";
    import { loginUser, authenticateUser } from "../../services/loginService";
    import { normalizeResponse } from "../../api/http.svelte";
    import { handleGlobalApiError } from "../../api/globalErrorHandler.svelte";

    let email = "";
    let password = "";
    let isFetching = $state(false);

    $effect(() => {
        const checkAuth = async () => {
            const authToken = getValue("authToken");

            if (authToken) {
                const { resp, body } = await authenticateUser(authToken);
                const normalizedResponse = normalizeResponse(resp);

                if (handleGlobalApiError(normalizedResponse)) return;
                if (!body || !normalizedResponse.ok) return;

                Object.assign(auth, { token: authToken });
                Object.assign(user, { email: body.email });

                if (body.changePassword) {
                    clearValue("authToken");
                    setValue("authToken_BCPW", authToken);
                    await push(`/changePassword?firstLogin=${body.firstLogin}`);
                    return;
                }

                await push("/dashboard");
            }
        };
        checkAuth();
    });

    /**
     * Handles user login process
     */
    async function btnLogin() {
        if (!email) {
            addToast({
                title: "Email darf nicht leer sein",
                type: "error"
            });
            return;
        }

        if (!password) {
            addToast({
                title: "Password darf nicht leer sein!",
                type: "error"
            });
            return;
        }

        if (isFetching) return;
        isFetching = true;

        try {
            const { resp, body } = await loginUser(email, password);
            const normalizedResponse = normalizeResponse(resp);

            if (!normalizedResponse.ok) {
                if (normalizedResponse.errorType === "REQUESTTIMEOUT") {
                    addToast({
                        title: "Zu viele Anmeldeversuche",
                        type: "warning",
                    });
                    return;
                }

                if (handleGlobalApiError(normalizedResponse)) return;

                return;
            }

            if (!body?.authToken) {
                addToast({
                    title: "Anmeldung fehlgeschlagen",
                    type: "error",
                });
                return;
            }

            Object.assign(auth, { token: body.authToken });
            Object.assign(user, { email: email });

            if (body.changePassword) {
                setValue("authToken_BCPW", body.authToken);
                await push(`/changePassword?firstLogin=${body.firstLogin}`);
            } else {
                setValue("authToken", body.authToken);
                await push("/dashboard");
            }
        } finally {
            isFetching = false;
        }
    }
</script>

<ToastStack isMobile={true} />

<main class="bg-gv-bg-bar w-dvw h-dvh flex items-center justify-center">
    <div class="p-10 h-full w-full bg-white flex flex-col items-center">
        <img
            src={Logo}
            alt="Logo"
            class="min-[530px]:w-1/5 max-[530px]:w-2/5 rounded-full border-2 border-gv-border"
        />
        <p class="font-semibold text-dt-1 mt-5 text-center">GVW Office</p>
        <p class="text-gv-light-text text-dt-3 text-center">
            Gesangverein Weppersdorf
        </p>

        <Input
            bind:value={email}
            type="email"
            placeholder="ihre.email@email.com"
            title="E-Mail"
            marginTop="5"
        />

        <Input
            bind:value={password}
            type="password"
            placeholder=""
            title="Passwort"
            marginTop="10"
        />

        <Button
            type="primary"
            marginTop="10"
            onclick={btnLogin}
            isSubmit={true}
            disabled={isFetching}
        >
            <span class="material-symbols-rounded">login</span>
            <p class="ml-3">Anmelden</p>
        </Button>
    </div>
</main>